package jfr.logging;

import jfr.event.NonReentrantMethodEvent;

/**
 * Регистрирует в лог и журнал Java Flight Recorder статистику времени выполнения бизнес-метода.
 *
 * <p>В отличие от {@link JfrLoggingService} не столь критично, если не будет завершающего вызова
 * {@link #afterReturning(Class, Object)} или {@link #afterThrowing(Class, Throwable)}.
 *
 * <p>@link LoggingService#proceed(ProceedingJoinPoint)} и {@link JfrLoggingService#proceedCallback(LoggingJoinPoint, JoinPointCallback)}
 * обеспечивают гарантию завершающих вызов внутри своей реализации. Если не порядок вызовов не будет соблюдаться, возникнет утечка памяти.</p>
 *
 * <p>Тут пришлось пожертвовать возможностью повторного вхождения в метод.</p>
 *
 * @param <E> тип события
 * @author Roman_Erzhukov
 */
public interface NonReentrantLoggingService<E extends NonReentrantMethodEvent> {
    /**
     * Выполняется перед вызовом метода.
     *
     * @param joinPoint вызываемая операция
     * @param event     событие метода
     */
    void before(LoggingJoinPoint joinPoint, E event);

    /**
     * Выполняется после успешного завершения метода.
     *
     * @param eventClass класс события
     * @param retVal     результат выполнения метода
     */
    void afterReturning(Class<E> eventClass, Object retVal);

    /**
     * Выполняется после ошибочного завершения метода.
     *
     * @param eventClass класс события
     * @param cause      причина ошибки
     */
    void afterThrowing(Class<E> eventClass, Throwable cause);
}
