package jfr.logging;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Function;

/**
 * Регистрирует в лог и журнал Java Flight Recorder статистику времени выполнения бизнес-метода.
 *
 * @param <E> тип события
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
public final class JfrLoggingServiceImpl<E extends NonReentrantMethodEvent> implements JfrLoggingService, NonReentrantLoggingService<E> {
    @VisibleForTesting
    static final ThreadLocal<LoggingContext> context = new ThreadLocal<>();

    private final Ticker ticker;
    private final Function<Class<?>, Logger> loggerFactory;

    /**
     * Позволяет включать дополнительную запись stacktrace-ов ошибок.
     *
     * <p>Обычно нет необходимости включать.
     * За логирование исключения отвечает перехвативший её код, получится двойная запись в лог.
     * Может помочь, если код подавляет исключения.</p>
     */
    @Value("${jfr.logErrorEnabled:false}")
    @VisibleForTesting
    boolean logErrorEnabled;

    /**
     * Устанавливает пороговую длительность для записи в JFR, нс.
     */
    @Value("${jfr.thresholdNanos:10000000}")
    @VisibleForTesting
    long thresholdNanos;

    @Override
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        var point = LoggingJoinPoint.of(joinPoint);
        LoggingContext context = doBefore(point, true, new MethodInvocationEvent(), log);
        if (context == null) {
            return joinPoint.proceed();
        }
        try {
            Object result = joinPoint.proceed();
            doAfterReturning(context, point, result);
            return result;
        } catch (Throwable t) {
            doAfterThrowing(context, point, t);
            throw t;
        }
    }

    @Override
    public Object proceedCallback(LoggingJoinPoint joinPoint, JoinPointCallback callback) throws Throwable {
        LoggingContext context = doBefore(joinPoint, true, new MethodInvocationEvent(), log);
        if (context == null) {
            return callback.proceed();
        }
        try {
            Object result = callback.proceed();
            doAfterReturning(context, joinPoint, result);
            return result;
        } catch (Throwable t) {
            doAfterThrowing(context, joinPoint, t);
            throw t;
        }
    }

    @Override
    public void before(LoggingJoinPoint joinPoint, E event) {
        log.trace("before {} {}", joinPoint, event);
        doBefore(joinPoint, false, event, log);
    }

    @VisibleForTesting
    LoggingContext doBefore(LoggingJoinPoint joinPoint, boolean methodInvocationEvent, AbstractMethodEvent event, Logger logger) {
        log.trace("doBefore - start {} {}", joinPoint, event);
        LoggingContext current = getContext();
        boolean debugEnabled = logger.isDebugEnabled();
        boolean eventEnabled = event.isEnabled();
        if (!debugEnabled && !eventEnabled || current == null && !methodInvocationEvent) { // Первым вызовом обязан идти MethodInvocationEvent.
            // NonReentrantMethodEvent предназначен для случаев, когда невозможно обеспечить гарантию вызова afterReturning/afterThrowable.
            // Когда вызова afterReturning/afterThrowable нет, то очистка контекста может
            // не выполниться вовсе, что приведёт к утечке памяти. Лучше не записывать в лог и JFR вовсе, чем вызвать утечку.
            removeContext();
            log.trace("doBefore - end {} {}: debugEnabled={}, eventEnabled={}, context={} => null",
                    joinPoint, event, debugEnabled, eventEnabled, current);
            return null;
        }
        LoggingContext context = current != null ? current : createContext(joinPoint);
        Class<?> targetClass = joinPoint.targetClass();
        var callback = new LoggingCallback(
                joinPoint,
                eventEnabled ? event : null,
                debugEnabled ? loggerFactory.apply(targetClass) : null,
                logErrorEnabled,
                joinPoint.name()
                        .toString(),
                targetClass,
                joinPoint.method());
        if (methodInvocationEvent) {
            context.before(callback, ticker);
        } else {
            context.beforeNonReentrant(callback, event);
        }
        if (current == null) {
            setContext(context);
        }
        log.trace("doBefore - end {} {}: debugEnabled={}, eventEnabled={} => {}", joinPoint, event, debugEnabled, eventEnabled, context);
        return context;
    }

    @VisibleForTesting
    LoggingContext createContext(LoggingJoinPoint joinPoint) {
        return new LoggingContext(joinPoint, thresholdNanos);
    }

    @Override
    public void afterReturning(Class<E> eventClass, Object retVal) {
        LoggingContext context = getContext();
        log.trace("afterReturning eventClass={} context={}", eventClass, context);
        if (context != null) {
            context.afterReturningNonReentrant(eventClass, retVal);
        }
    }

    @VisibleForTesting
    void doAfterReturning(LoggingContext context, LoggingJoinPoint joinPoint, Object retVal) {
        log.trace("doAfterReturning {} {}", joinPoint, context);
        if (context.afterReturning(joinPoint, retVal)) {
            removeContext();
        }
    }

    @Override
    public void afterThrowing(Class<E> eventClass, Throwable cause) {
        LoggingContext context = getContext();
        log.trace("afterThrowing eventClass={} context={}", eventClass, context);
        if (context != null) {
            context.afterThrowingNonReentrant(eventClass, cause);
        }
    }

    @VisibleForTesting
    void doAfterThrowing(LoggingContext context, LoggingJoinPoint joinPoint, Throwable cause) {
        log.trace("doAfterThrowing {} {}", context, joinPoint);
        if (context.afterThrowing(joinPoint, cause)) {
            removeContext();
        }
    }

    @VisibleForTesting
    LoggingContext getContext() {
        return context.get();
    }

    @VisibleForTesting
    void setContext(LoggingContext ctx) {
        log.trace("setContext {}", ctx);
        context.set(ctx);
    }

    @VisibleForTesting
    void removeContext() {
        log.trace("removeContext");
        context.remove();
    }
}
