package jfr.logging;

import jfr.api.LoggingJoinPoint;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Создаёт контекст для записи в лог или JFR.
 */
@RequiredArgsConstructor
class JfrLoggingContextFactory implements Function<LoggingJoinPoint, LoggingContext> {
    private final JfrLoggingProperties properties;

    @Override
    public LoggingContext apply(LoggingJoinPoint joinPoint) {
        return new LoggingContext(joinPoint, properties.threshold().toNanos());
    }
}
