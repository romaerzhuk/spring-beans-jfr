package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.api.JfrLoggingService;
import jfr.api.JoinPointCallback;
import jfr.api.NonReentrantLoggingService;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Регистрирует в лог и журнал Java Flight Recorder статистику времени выполнения бизнес-метода.
 *
 * @param <E> тип события
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
final class JfrLoggingServiceImpl<E extends NonReentrantMethodEvent> implements JfrLoggingService, NonReentrantLoggingService<E> {
    private final JfrLoggingHelper helper;
    private final JfrLoggingContextHolder contextHolder;

    @Override
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return proceedCallback(contextHolder.wrap(joinPoint), joinPoint::proceed);
    }

    @Override
    public Object proceedCallback(JfrJoinPoint joinPoint, JoinPointCallback callback) throws Throwable {
        log.trace("proceedCallback - start {}", joinPoint);
        LoggingContext context = helper.before(joinPoint, new MethodInvocationEvent(), log);
        if (context == null) {
            return callback.proceed();
        }
        try {
            Object result = callback.proceed();
            helper.afterReturning(context, joinPoint, result);
            log.trace("proceedCallback - end {}: return {}", joinPoint, result);
            return result;
        } catch (Throwable t) {
            helper.afterThrowing(context, joinPoint, t);
            log.trace("proceedCallback - end {}: throw {}", joinPoint, t.toString());
            throw t;
        }
    }

    @Override
    public void before(JfrJoinPoint joinPoint, E event) {
        log.trace("before {} {}", joinPoint, event);
        helper.before(joinPoint, event, log);
    }

    @Override
    public void afterReturning(Class<E> eventClass, Object retVal) {
        LoggingContext context = contextHolder.getContext();
        log.trace("afterReturning eventClass={} context={}", eventClass, context);
        if (context != null) {
            context.afterReturningNonReentrant(eventClass, retVal);
        }
    }

    @Override
    public void afterThrowing(Class<E> eventClass, Throwable cause) {
        LoggingContext context = contextHolder.getContext();
        log.trace("afterThrowing eventClass={} context={}", eventClass, context);
        if (context != null) {
            context.afterThrowingNonReentrant(eventClass, cause);
        }
    }
}
