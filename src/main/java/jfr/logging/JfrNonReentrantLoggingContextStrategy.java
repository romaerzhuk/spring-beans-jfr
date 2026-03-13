package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.NonReentrantMethodEvent;

import java.util.function.Function;

/**
 * Стратегия создания и инициализации контекста {@link LoggingContext} для {@link NonReentrantMethodEvent}.
 *
 * @author Roman_Erzhukov
 */
final class JfrNonReentrantLoggingContextStrategy implements JfrLoggingContextStrategy {
    @Override
    public LoggingContext createIfReentrant(LoggingJoinPoint joinPoint, Function<LoggingJoinPoint, LoggingContext> factory) {
        return null;
    }

    @Override
    public LoggingContext init(LoggingContext context, LoggingCallback callback, AbstractMethodEvent event) {
        context.beforeNonReentrant(callback, event);
        return context;
    }
}
