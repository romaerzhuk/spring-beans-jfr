package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.springframework.core.log.LogMessage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Контекст регистрации событий выполнения методов.
 *
 * @param logger                     логгер
 * @param thresholdNanos             временной порог срабатывания записи статистики в лог или JFR, нс
 * @param statistics                 статистика
 * @param callbacks                  стек вызовов
 * @param noReentrantCallbackByClass вызовы по классам событий, которые не могут вызывать внутри себя другие вызовы
 * @author Roman_Erzhukov
 */
@Slf4j
record LoggingContext(
        Logger logger,
        long thresholdNanos,
        Map<Key, LoggingStatistic> statistics,
        LoggingCallbackStack callbacks,
        NoReentrantCallbackByEventClass noReentrantCallbackByClass) {

    /**
     * Ключ хранения статистики.
     *
     * @param beanClass класс компонента Spring
     * @param method    метод
     */
    record Key(Class<?> beanClass, Object method) {
        static Key of(Class<?> beanClass, Object method) {
            return new Key(beanClass, method);
        }
    }

    /**
     * Создаёт контекст регистрации событий выполнения методов.
     *
     * @param thresholdNanos пороговая длительность для записи в JFR, нс
     */
    public LoggingContext(long thresholdNanos) {
        this(log, thresholdNanos, new HashMap<>(), new LoggingCallbackStack(), new NoReentrantCallbackByEventClass());
    }

    /**
     * Выполняется перед выполнением бизнес-метода
     *
     * @param callback выполняет регистрацию
     */
    public void before(LoggingCallback callback) {
        log.trace("before {} {}", this, callback);
        LoggingCallback prev = callbacks.peekLast();
        callbacks.add(callback);
        if (prev != null) {
            prev.suspend();
        }
        callback.resume();
        callback.beginEvent();
        callback.beginLogger();
    }

    /**
     * Выполняется перед выполнением бизнес-метода
     *
     * @param callback выполняет регистрацию
     * @param event    событие
     */
    public void beforeNonReentrant(LoggingCallback callback, AbstractMethodEvent event) {
        log.trace("beforeNonReentrant {} {} - start", this, event);
        noReentrantCallbackByClass.put(event.getClass(), callback);
        callback.beginEvent();
        callback.beginLogger();
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param joinPoint точка вызова
     * @param retVal    результат метода
     * @return признак последнего фрейма стека
     */
    public boolean afterReturning(JfrJoinPoint joinPoint, Object retVal) {
        log.trace("afterReturning {} {}", this, joinPoint);
        noReentrantCallbackByClass.removeIfIndexGreaterOrEqual(joinPoint);
        LoggingCallback callback = callbacks.removeIfIndexGreaterOrEqual(joinPoint, this);
        if (callback == null) {
            return true;
        }
        callback.logSuccess(retVal);
        return joinPoint.index() == 0;
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param eventClass класс события JFR
     * @param retVal     результат метода
     */
    public void afterReturningNonReentrant(Class<? extends NonReentrantMethodEvent> eventClass, Object retVal) {
        log.trace("afterReturningNonReentrant {} {}", this, eventClass);
        LoggingCallback callback = noReentrantCallbackByClass.remove(eventClass);
        if (callback == null) {
            return;
        }
        noReentrantCallbackByClass.removeIfIndexGreaterOrEqual(callback.joinPoint());
        callback.endEvent();
        callback.logSuccess(retVal);
    }

    /**
     * Выполняется после ошибки выполнения метода.
     *
     * @param joinPoint точка вызова
     * @param cause     причина ошибки
     * @return признак последнего фрейма стека
     */
    public boolean afterThrowing(JfrJoinPoint joinPoint, Throwable cause) {
        log.trace("afterThrowing {} {}", this, joinPoint);
        noReentrantCallbackByClass.removeIfIndexGreaterOrEqual(joinPoint);
        LoggingCallback callback = callbacks.removeIfIndexGreaterOrEqual(joinPoint, this);
        if (callback == null) {
            return true;
        }
        callback.logFailure(cause);
        return joinPoint.index() == 0;
    }

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param eventClass класс события JFR
     * @param cause      причина ошибки
     */
    public void afterThrowingNonReentrant(Class<? extends NonReentrantMethodEvent> eventClass, Throwable cause) {
        log.trace("afterThrowingNonReentrant {} {}", this, eventClass);
        LoggingCallback callback = noReentrantCallbackByClass.remove(eventClass);
        if (callback == null) {
            return;
        }
        noReentrantCallbackByClass.removeIfIndexGreaterOrEqual(callback.joinPoint());
        callback.endEvent();
        callback.logFailure(cause);
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
