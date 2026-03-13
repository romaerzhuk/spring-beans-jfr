package jfr.logging;

import com.google.common.base.Ticker;
import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Стратегия создания и инициализации контекста {@link LoggingContext} для {@link jfr.event.MethodInvocationEvent}.
 *
 * @author Roman_Erzhukov
 */
@RequiredArgsConstructor
final class JfrReentrantLoggingContextStrategy implements JfrLoggingContextStrategy {
    private final Ticker ticker;

    @Override
    public LoggingContext createIfReentrant(LoggingJoinPoint joinPoint, Function<LoggingJoinPoint, LoggingContext> factory) {
        return factory.apply(joinPoint);
    }

    @Override
    public LoggingContext init(LoggingContext context, LoggingCallback callback, AbstractMethodEvent event) {
        context.before(callback, ticker);
        return context;
    }
}
