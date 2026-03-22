package jfr.logging;

import jfr.api.JfrJoinPoint;

import java.util.function.Function;

/**
 * Стратегия создания и инициализации контекста {@link LoggingContext} для {@link jfr.event.MethodInvocationEvent}.
 *
 * @author Roman_Erzhukov
 */
final class JfrReentrantLoggingContextStrategy implements JfrLoggingContextStrategy {
    @Override
    public LoggingContext createContextIfReentrant(JfrJoinPoint joinPoint, Function<JfrJoinPoint, LoggingContext> factory) {
        return factory.apply(joinPoint);
    }
}
