package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import java.util.function.Function;

/**
 * Хранит контекст регистрации событий выполнения методов.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
final class JfrLoggingContextHolder implements DisposableBean {
    private final ThreadLocal<LoggingContext> context;
    private final Function<LoggingJoinPoint, LoggingContext> contextFactory;
    private final LoggingCallbackFactory callbackFactory;

    /**
     * Возвращает текущий контекст, или создаёт новый.
     *
     * @param joinPoint точка вызова
     * @param event     событие JFR
     * @param logger    логгер
     * @param strategy  стратения создания контекста
     * @return {@link LoggingContext} или null
     */
    @Nullable
    public LoggingContext getOrCreateIfReentrant(LoggingJoinPoint joinPoint,
                                                 AbstractMethodEvent event,
                                                 Logger logger,
                                                 JfrLoggingContextStrategy strategy) {
        LoggingContext context = getContext();
        if (context == null) {
            context = strategy.createIfReentrant(joinPoint, contextFactory);
            if (context == null) {
                // NonReentrantMethodEvent предназначен для случаев, когда невозможно обеспечить гарантию вызова afterReturning/afterThrowable.
                // Когда вызова afterReturning/afterThrowable нет, то очистка контекста может
                // не выполниться вовсе, что приведёт к утечке памяти. Лучше не записывать в лог и JFR вовсе, чем вызвать утечку.
                return null;
            }
            setContext(context);
        }
        LoggingCallback callback = callbackFactory.create(joinPoint, event, logger);
        return strategy.init(context, callback, event);
    }

    /**
     * Возвращает текущий контекст.
     *
     * @return {@link LoggingContext}
     */
    LoggingContext getContext() {
        return context.get();
    }

    /**
     * Устанавливает текущий контекст.
     *
     * @param context контекст
     */
    public void setContext(LoggingContext context) {
        log.trace("setContext {}", context);
        this.context.set(context);
    }

    @Override
    public void destroy() {
        log.info("destroy");
        context.remove();
    }

    /**
     * Удаляет контекст.
     */
    public void removeContext() {
        log.trace("remove context");
        context.remove();
    }
}
