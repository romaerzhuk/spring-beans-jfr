package jfr.logging;

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
    private final JfrLoggingProperties properties;

    /**
     * Создаёт {@link LoggingCallback} для регистрации в лог и JFR.
     *
     * @param joinPoint точка вызова
     * @param event     событие JFR
     * @param logger    логгер
     * @param context   контекст регистрации событий
     * @param strategy  стратегия инициализации
     * @return {@link LoggingCallback}
     */
    public LoggingCallback create(JfrJoinPoint joinPoint,
                                  AbstractMethodEvent event,
                                  Logger logger,
                                  LoggingContext context,
                                  JfrLoggingContextStrategy strategy) {
        Class<?> targetClass = joinPoint.targetClass();
        boolean debugEnabled = logger.isDebugEnabled();
        Object name = joinPoint.name();
        return new LoggingCallback(
                joinPoint,
                event.isEnabled() ? event : null,
                debugEnabled ? loggerFactory.apply(targetClass) : null,
                properties.logErrorEnabled(),
                name.toString(),
                targetClass,
                joinPoint.method(),
                context.callbacks().size(),
                strategy.createUnstartedStopwatchOrNull(context),
                debugEnabled ? joinPoint.args() : null);
    }
}
