package jfr.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import java.util.function.Function;

/**
 * Создаёт {@link LoggingCallback} для регистрации в лог и JFR.
 *
 * @author Roman_Erzhukov
 */
@RequiredArgsConstructor
final class LoggingCallbackFactory {
    private final Function<Class<?>, Logger> loggerFactory;
    private final Ticker ticker;
    private final JfrLoggingProperties properties;

    /**
     * Создаёт {@link LoggingCallback} для регистрации в лог и JFR.
     *
     * @param joinPoint точка вызова
     * @param event     событие JFR
     * @param logger    логгер
     * @param context   контекст регистрации событий
     * @return {@link LoggingCallback}
     */
    public LoggingCallback create(JfrJoinPoint joinPoint, AbstractMethodEvent event, Logger logger, LoggingContext context) {
        Class<?> targetClass = joinPoint.targetClass();
        boolean debugEnabled = logger.isDebugEnabled();
        Object name = joinPoint.name();
        return new LoggingCallback(
                joinPoint,
                event,
                debugEnabled ? loggerFactory.apply(targetClass) : null,
                properties.logErrorEnabled(),
                name.toString(),
                targetClass,
                joinPoint.method(),
                context.callbacks().size(),
                Stopwatch.createUnstarted(ticker),
                debugEnabled ? joinPoint.args() : null);
    }
}
