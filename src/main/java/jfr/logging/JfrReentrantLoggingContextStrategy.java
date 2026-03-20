package jfr.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.api.JfrJoinPoint;
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
    public LoggingContext createContextIfReentrant(JfrJoinPoint joinPoint, Function<JfrJoinPoint, LoggingContext> factory) {
        return factory.apply(joinPoint);
    }

    @Override
    public Stopwatch createUnstartedStopwatchOrNull(LoggingContext context) {
        return Stopwatch.createUnstarted(ticker);
    }

    @Override
    public LoggingContext init(LoggingContext context, LoggingCallback callback, AbstractMethodEvent event) {
        context.before(callback);
        return context;
    }
}
