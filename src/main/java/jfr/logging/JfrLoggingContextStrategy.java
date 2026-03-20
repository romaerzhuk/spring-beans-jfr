package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Стратегия создания и инициализации контекста {@link LoggingContext}.
 *
 * @author Roman_Erzhukov
 */
interface JfrLoggingContextStrategy {
    /**
     * Создаёт контекст если {@link MethodInvocationEvent}, иначе возвращает null.
     *
     * @param joinPoint точка вызова
     * @param factory   фабрика создающиая контекст
     * @return контекст, или null
     */
    @Nullable
    LoggingContext createContextIfReentrant(JfrJoinPoint joinPoint, Function<JfrJoinPoint, LoggingContext> factory);

    /**
     * Создаёт остановленный {@link Stopwatch}, или возвращает null.
     *
     * @param context контекст
     * @return остановленный {@link Stopwatch}, или null.
     */
    @Nullable
    Stopwatch createUnstartedStopwatchOrNull(LoggingContext context);

    /**
     * Инициализирует контекст.
     *
     * @param context  контекст
     * @param callback вызов для регистрации в лог или JFR
     * @param event    событие
     * @return context
     */
    LoggingContext init(LoggingContext context, LoggingCallback callback, AbstractMethodEvent event);
}
