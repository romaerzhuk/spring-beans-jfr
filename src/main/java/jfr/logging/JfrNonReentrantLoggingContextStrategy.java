package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.NonReentrantMethodEvent;

import java.util.function.Function;

/**
 * Стратегия создания и инициализации контекста {@link LoggingContext} для {@link NonReentrantMethodEvent}.
 *
 * @author Roman_Erzhukov
 */
final class JfrNonReentrantLoggingContextStrategy implements JfrLoggingContextStrategy {
    @Override
    public LoggingContext createContextIfReentrant(JfrJoinPoint joinPoint, Function<JfrJoinPoint, LoggingContext> factory) {
        return null;
    }
}
