package jfr.logging;

import jfr.api.LoggingJoinPoint;
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
     * @return {@link LoggingCallback}
     */
    public LoggingCallback create(LoggingJoinPoint joinPoint, AbstractMethodEvent event, Logger logger) {
        Class<?> targetClass = joinPoint.targetClass();
        return new LoggingCallback(
                joinPoint,
                event.isEnabled() ? event : null,
                logger.isDebugEnabled() ? loggerFactory.apply(targetClass) : null,
                properties.logErrorEnabled(),
                joinPoint.name()
                        .toString(),
                targetClass,
                joinPoint.method());
    }
}
