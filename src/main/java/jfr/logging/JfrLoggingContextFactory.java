package jfr.logging;

import jfr.api.JfrJoinPoint;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Создаёт контекст для записи в лог или JFR.
 */
@RequiredArgsConstructor
class JfrLoggingContextFactory implements Function<JfrJoinPoint, LoggingContext> {
    private final JfrLoggingProperties properties;

    @Override
    public LoggingContext apply(JfrJoinPoint joinPoint) {
        return new LoggingContext(properties.threshold().toNanos());
    }
}
