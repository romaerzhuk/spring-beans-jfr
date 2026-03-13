package jfr.logging;

import jfr.api.LoggingJoinPoint;
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
    LoggingContext createIfReentrant(LoggingJoinPoint joinPoint, Function<LoggingJoinPoint, LoggingContext> factory);

    /**
     * Инифиализирует контекст.
     *
     * @param context  контекст
     * @param callback вызов для регистрации в лог или JFR
     * @param event    событие
     * @return context
     */
    LoggingContext init(LoggingContext context, LoggingCallback callback, AbstractMethodEvent event);
}
