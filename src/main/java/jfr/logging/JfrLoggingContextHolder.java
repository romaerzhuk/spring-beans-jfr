package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.api.JfrJoinPointFactory;
import jfr.event.AbstractMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.function.Function;

/**
 * Хранит контекст регистрации событий выполнения методов.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
final class JfrLoggingContextHolder implements JfrJoinPointFactory, DisposableBean {
    private final ThreadLocal<LoggingContext> context;
    private final Function<JfrJoinPoint, LoggingContext> contextFactory;
    private final LoggingCallbackFactory callbackFactory;

    @Override
    public JfrJoinPoint create(Class<?> targetClass, Object name, Object method, List<Object> args) {
        return new JfrJoinPointAdapter(index(), targetClass, name, method, args);
    }

    /**
     * Создаёт обёртку {@link JfrJoinPoint} для AspectJ {@link JoinPoint}.
     *
     * @param joinPoint точка вызова
     */
    public JfrJoinPoint wrap(JoinPoint joinPoint) {
        return new AspectJfrJoinPoint(index(), joinPoint);
    }

    private int index() {
        LoggingContext context = getContext();
        return context == null ? 0 : context.callbacks().size();
    }

    /**
     * Возвращает текущий контекст, или создаёт новый.
     *
     * @param joinPoint точка вызова
     * @param event     событие JFR
     * @param logger    логгер
     * @return {@link LoggingContext} или null
     */
    @Nullable
    public LoggingContext getOrCreateIfReentrant(JfrJoinPoint joinPoint, AbstractMethodEvent event, Logger logger) {
        LoggingContext context = getContext();
        if (context == null) {
            context = event.isReentrant() ? contextFactory.apply(joinPoint) : null;
            if (context == null) {
                // NonReentrantMethodEvent предназначен для случаев, когда невозможно обеспечить гарантию вызова afterReturning/afterThrowable.
                // Если вызова afterReturning/afterThrowable нет, то очистка контекста может
                // не выполниться вовсе, что приведёт к утечке памяти. Лучше не записывать в лог и JFR вовсе, чем вызвать утечку.
                return null;
            }
            setContext(context);
        }
        LoggingCallback callback = callbackFactory.create(joinPoint, event, logger, context);
        context.before(callback);
        return context;
    }

    /**
     * Возвращает текущий контекст.
     *
     * @return {@link LoggingContext}
     */
    public LoggingContext getContext() {
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
