package jfr.logging;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.springframework.core.log.LogMessage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Контекст регистрации событий выполнения методов.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
final class LoggingContext {
    @VisibleForTesting
    record Key(Class<?> beanClass, Object method) {
        static Key of(Class<?> beanClass, Object method) {
            return new Key(beanClass, method);
        }
    }

    @ToString.Include
    @VisibleForTesting
    final Object identityPoint;
    @VisibleForTesting
    final Logger logger;
    @VisibleForTesting
    final long thresholdNanos;
    @VisibleForTesting
    final HashMap<Key, LoggingStatistic> statistics = new HashMap<>();
    @VisibleForTesting
    final HashMap<Class<? extends AbstractMethodEvent>, LoggingCallback> callbackByNoReentrantEventClass = new HashMap<>();
    @VisibleForTesting
    final HashMap<Class<? extends AbstractMethodEvent>, Predicate<LoggingJoinPoint>> predicateByNoReentrantEventClass = new HashMap<>();

    @ToString.Include
    @VisibleForTesting
    LoggingCallback callback;

    /**
     * Создаёт контекст регистрации событий выполнения методов.
     *
     * @param joinPoint      точка вызова
     * @param thresholdNanos пороговая длительность для записи в JFR, нс
     */
    public LoggingContext(LoggingJoinPoint joinPoint, long thresholdNanos) {
        this(Objects.requireNonNullElse(joinPoint.identityPoint(), joinPoint), log, thresholdNanos);
    }

    /**
     * Выполняется перед выполнением бизнес-метода
     *
     * @param callback выполняет регистрацию
     * @param ticker   возвращает время для создания {@link Stopwatch}
     */
    public void before(LoggingCallback callback, Ticker ticker) {
        log.trace("before {} {}", this, callback);
        callback.before(this.callback, ticker);
        this.callback = callback;
    }

    /**
     * Выполняется перед выполнением бизнес-метода
     *
     * @param callback выполняет регистрацию
     * @param event    событие
     */
    @SuppressWarnings("unchecked")
    public void beforeNonReentrant(LoggingCallback callback, AbstractMethodEvent event) {
        log.trace("beforeNonReentrant {} {} - start", this, event);
        callback.before(null, null);
        callbackByNoReentrantEventClass.put(event.getClass(), callback);
        if (event instanceof Predicate<?> predicate) {
            predicateByNoReentrantEventClass.put(event.getClass(), (Predicate<LoggingJoinPoint>) predicate);
        }
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param joinPoint точка вызова
     * @param retVal    результат метода
     * @return признак последнего фрейма стека
     */
    public boolean afterReturning(LoggingJoinPoint joinPoint, Object retVal) {
        log.trace("afterReturning {} {}", this, joinPoint);
        return after(callback.afterReturning(this, retVal), joinPoint);
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param eventClass класс события JFR
     * @param retVal     результат метода
     */
    public void afterReturningNonReentrant(Class<? extends NonReentrantMethodEvent> eventClass, Object retVal) {
        log.trace("afterReturningNonReentrant {} {}", this, eventClass);
        LoggingCallback callback = callbackByNoReentrantEventClass.remove(eventClass);
        predicateByNoReentrantEventClass.remove(eventClass);
        if (callback != null) {
            callback.afterReturning(this, retVal);
        }
    }

    /**
     * Выполняется после ошибки выполнения метода.
     *
     * @param joinPoint точка вызова
     * @param cause     причина ошибки
     * @return признак последнего фрейма стека
     */
    public boolean afterThrowing(LoggingJoinPoint joinPoint, Throwable cause) {
        log.trace("afterThrowing {} {}", this, joinPoint);
        return after(callback.afterThrowing(this, cause), joinPoint);
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param eventClass класс события JFR
     * @param cause      причина ошибки
     */
    public void afterThrowingNonReentrant(Class<? extends NonReentrantMethodEvent> eventClass, Throwable cause) {
        log.trace("afterThrowingNonReentrant {} {}", this, eventClass);
        LoggingCallback callback = callbackByNoReentrantEventClass.remove(eventClass);
        predicateByNoReentrantEventClass.remove(eventClass);
        if (callback != null) {
            callback.afterThrowing(this, cause);
        }
    }

    @VisibleForTesting
    boolean after(LoggingCallback callback, LoggingJoinPoint joinPoint) {
        log.trace("after - start {} {} {}", this, callback, joinPoint);
        tryAfterNoReentrant(joinPoint);
        this.callback = callback;
        if (callback == null) {
            log.trace("after - end {} callback=null {} => true", this, joinPoint);
            return true;
        }
        Object point = joinPoint.identityPoint();
        if (identityPoint == point) { // Предохранитель, workaround на случай потери вызовов. Не очень надёжный, но лучше не придумал.
            logger.error("При вызове {} не все вложенные операции завершились." +
                    " Не соблюдается соответствие вызовов before/afterReturning или before/afterThrowing." +
                    " Часть статистики JFR потеряна", joinPoint);
            log.trace("after - end {} {} {}: identityPoint={} == point={} => true", this, callback, joinPoint, identityPoint, point);
            return true;
        }
        log.trace("after - end {} {} {} => false", this, callback, joinPoint);
        return false;
    }

    @VisibleForTesting
    void tryAfterNoReentrant(LoggingJoinPoint joinPoint) {
        log.trace("tryAfterNoReentrant {} - start", joinPoint);
        predicateByNoReentrantEventClass.entrySet()
                .removeIf(entry -> {
                    Predicate<LoggingJoinPoint> predicate = entry.getValue();
                    if (!predicate.test(joinPoint)) {
                        log.trace("tryAfterNoReentrant {} test {} = false", joinPoint, entry);
                        return false;
                    }
                    Class<? extends AbstractMethodEvent> eventClass = entry.getKey();
                    LoggingCallback callback = callbackByNoReentrantEventClass.remove(eventClass);
                    log.trace("tryAfterNoReentrant {}, test {} = true, callback = {}", joinPoint, eventClass, callback);
                    if (callback != null) {
                        callback.afterReturning(LoggingContext.this, null);
                    }
                    return true;
                });
    }

    @VisibleForTesting
    Object getIdentityPoint(LoggingJoinPoint joinPoint) {
        return Objects.requireNonNullElse(joinPoint.identityPoint(), joinPoint);
    }

    /**
     * Возвращает статистику выполнения метода.
     *
     * @param clazz  класс Spring-бина
     * @param method метод
     * @return {@link LoggingStatistic}
     */
    public LoggingStatistic getStatistic(Class<?> clazz, Object method) {
        return statistics.computeIfAbsent(Key.of(clazz, method), key -> new LoggingStatistic());
    }

    /**
     * Фиксирует статистику в журнал Java Flight Recorder.
     *
     * @param event событие
     */
    public void commit(MethodInvocationEvent event) {
        if (event.max < thresholdNanos) {
            return;
        }
        event.count = 1;
        event.min = event.max;
        event.sum = event.max;
        event.avg = event.max;
        event.commit();
        statistics.values()
                .stream()
                .filter(s -> s.getEvent() != event)
                .forEach(LoggingStatistic::commit);
    }

    /**
     * Статистика для логирования.
     */
    public LogMessage toStatistics() {
        return LogMessage.of(() -> {
            var sb = new StringBuilder();
            long time = statistics.values()
                    .stream()
                    .mapToLong(LoggingStatistic::getSum)
                    .sum();
            sb.append(DurationFormatUtils.formatDurationHMS(time / 1000_000));
            statistics.entrySet()
                    .stream()
                    .sorted(Comparator.<Map.Entry<Key, LoggingStatistic>>comparingLong(e -> e.getValue().getSum()).reversed())
                    .forEach(p -> p.getValue().appendTo(sb, p.getKey().beanClass(), p.getKey().method()));
            return sb.toString();
        });
    }
}
