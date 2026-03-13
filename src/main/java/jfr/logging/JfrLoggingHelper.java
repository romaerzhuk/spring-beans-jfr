package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Вспомогательный компонент для записи событий в лог и JFR.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
class JfrLoggingHelper {
    private final JfrLoggingContextHolder contextHolder;

    /**
     * Возвращает {@link LoggingContext} перед запуском целевого метода, или null.
     *
     * @param joinPoint точка вызова
     * @param strategy  стратегия создания контекста
     * @param event     событие JFR
     * @param logger    логгер
     * @return {@link LoggingContext} или null
     */
    @Nullable
    public LoggingContext before(LoggingJoinPoint joinPoint, JfrLoggingContextStrategy strategy, AbstractMethodEvent event, Logger logger) {
        if (!event.isEnabled(joinPoint, logger)) {
            log.trace("before {} {} - disabled", joinPoint, event);
            return null;
        }
        LoggingContext context = contextHolder.getOrCreateIfReentrant(joinPoint, event, logger, strategy);
        log.trace("before {} {} => {}", joinPoint, event, context);
        return context;
    }

    /**
     * Вызывается после успешного выполнения целевого метода.
     *
     * @param context   контекст регистрации событий JFR и логгирования
     * @param joinPoint точка вызвова
     * @param retVal    результат
     */
    public void afterReturning(LoggingContext context, LoggingJoinPoint joinPoint, Object retVal) {
        log.trace("afterReturning {} {}", joinPoint, context);
        if (context.afterReturning(joinPoint, retVal)) {
            contextHolder.removeContext();
        }
    }

    /**
     * Вызывается после выбрасывания исключения в результате выполнения целевого метода
     *
     * @param context   контекст регистрации событий JFR и логгирования
     * @param joinPoint точка вызова
     * @param cause     причина ошибки
     */
    public void afterThrowing(LoggingContext context, LoggingJoinPoint joinPoint, Throwable cause) {
        log.trace("afterThrowing {} {}", context, joinPoint);
        if (context.afterThrowing(joinPoint, cause)) {
            contextHolder.removeContext();
        }
    }
}