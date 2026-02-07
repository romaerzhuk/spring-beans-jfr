package jfr.logging;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Вызывается для регистрации в лог и журнал Java Flight Recorder статистики времени выполнения бизнес-метода.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("LoggingSimilarMessage")
final class LoggingCallback {
    @VisibleForTesting
    final LoggingJoinPoint joinPoint;
    @Nullable
    @VisibleForTesting
    final AbstractMethodEvent event;
    @Nullable
    @VisibleForTesting
    final Logger logger;
    @VisibleForTesting
    final boolean logErrorEnabled;
    @VisibleForTesting
    final String name;
    @ToString.Include
    @VisibleForTesting
    final Class<?> targetClass;
    @VisibleForTesting
    final Object method;

    @Nullable
    @VisibleForTesting
    LoggingCallback prev;
    @VisibleForTesting
    Stopwatch stopwatch;
    @VisibleForTesting
    List<Object> args;

    /**
     * Выполняется перед вызовом бизнес-метода.
     *
     * @param prev   предыдущий обработчик
     * @param ticker возвращает время для создания {@link Stopwatch}
     */
    public void before(LoggingCallback prev, Ticker ticker) {
        if (ticker != null) {
            start(prev, ticker);
        }
        beginEvent();
        beginLogger();
    }

    /**
     * Выполняется в случае успешного выполнения бизнес-метода.
     *
     * @param context контекст регистрации событий
     * @param retVal  результат выполнения метода
     * @return предыдущий обработчик, или null
     */
    public LoggingCallback afterReturning(LoggingContext context, Object retVal) {
        if (stopwatch != null) {
            var e = (MethodInvocationEvent) event;
            stop(context, e);
            logSuccess(retVal);
            return after(context, e);
        }
        log.trace("afterReturning event={} commit", event);
        if (event != null) {
            event.commit();
        }
        logSuccess(retVal);
        return null;
    }

    @VisibleForTesting
    void logSuccess(Object retVal) {
        if (logger != null) {
            logger.debug("{} end {}: {} {}", name, args, stopwatch(), retVal);
        }
    }

    /**
     * Вызывается в случае ошибки выполнения бизнес-метода.
     *
     * @param context контекст регистрации событий
     * @param thrown  исключение
     * @return предыдущий обработчик, или null
     */
    public LoggingCallback afterThrowing(LoggingContext context, Throwable thrown) {
        if (stopwatch != null) {
            var e = (MethodInvocationEvent) event;
            stop(context, e);
            logFailure(thrown);
            return after(context, e);
        }
        log.trace("afterThrowing event={} end", event);
        if (event != null) {
            event.end();
        }
        logFailure(thrown);
        return null;
    }

    @VisibleForTesting
    LoggingCallback after(LoggingContext context, MethodInvocationEvent event) {
        endEvent(event);
        if (prev == null) {
            collectStatistic(context, event);
        }
        return prev;
    }

    @VisibleForTesting
    void start(LoggingCallback prev, Ticker ticker) {
        this.prev = prev;
        if (prev != null) {
            prev.suspend();
        }
        stopwatch = Stopwatch.createStarted(ticker);
    }

    @VisibleForTesting
    void suspend() {
        stopwatch.stop();
    }

    @VisibleForTesting
    void beginEvent() {
        log.trace("beginEvent {} - start", event);
        if (event != null) {
            event.beanClass = targetClass;
            event.method = name;
            event.begin();
        }
        log.trace("beginEvent {} - end", event);
    }

    @VisibleForTesting
    void beginLogger() {
        if (logger != null) {
            args = joinPoint.args();
            logger.debug("{} start {}", name, args);
        }
    }

    @VisibleForTesting
    void stop(LoggingContext context, MethodInvocationEvent event) {
        if (stopwatch == null || !stopwatch.isRunning()) {
            return;
        }
        stopwatch.stop();
        if (prev != null) {
            prev.resume();
        }
        context.getStatistic(targetClass, method)
                .update(stopwatch, event);
    }

    @VisibleForTesting
    void resume() {
        stopwatch.start();
    }

    @VisibleForTesting
    void logFailure(Throwable thrown) {
        if (logger == null) {
            return;
        }
        if (logErrorEnabled) {
            logger.error("{} end {}: {}", name, args, stopwatch(), thrown);
        } else {
            logger.debug("{} end {}: {} {}", name, args, stopwatch(), thrown.toString());
        }
    }

    private Object stopwatch() {
        return stopwatch == null ? "" : stopwatch;
    }

    @VisibleForTesting
    void endEvent(MethodInvocationEvent event) {
        log.trace("endEvent {} - start", event);
        if (event != null) {
            event.max = stopwatch.elapsed(TimeUnit.NANOSECONDS);
            event.end();
        }
        log.trace("endEvent {} - end", event);
    }

    @VisibleForTesting
    void collectStatistic(LoggingContext context, MethodInvocationEvent event) {
        if (logger != null) {
            logger.debug("{} {} {} statistics: {}", targetClass.getSimpleName(), method, args, context.toStatistics());
        }
        if (event != null) {
            context.commit(event);
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        appendTo(sb, true);
        return sb.toString();
    }

    private void appendTo(StringBuilder sb, boolean withPrev) {
        sb.append("LoggingCallback{joinPoint=")
                .append(joinPoint)
                .append(", event=")
                .append(event)
                .append(", name=")
                .append(name)
                .append(", targetClass=")
                .append(targetClass.getSimpleName());
        if (withPrev) {
            sb.append(", prev=");
            if (prev == null) {
                sb.append("null");
            } else {
                prev.appendTo(sb, false);
            }
        }
        sb.append('}');
    }
}
