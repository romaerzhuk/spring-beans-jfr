package jfr.api;

import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Регистрирует в лог и журнал Java Flight Recorder выполнение бизнес-метода.
 *
 * <p>В отличие от {@link JfrLoggingService} не столь критично, если не будет завершающего вызова
 * {@link #afterReturning(Class, Object)} или {@link #afterThrowing(Class, Throwable)}.
 *
 * <p>{@link JfrLoggingService#proceed(ProceedingJoinPoint)} и {@link JfrLoggingService#proceedCallback(JfrJoinPoint, JoinPointCallback)}
 * обеспечивают гарантию завершающих вызов внутри своей реализации. Если порядок вызовов не будет соблюдаться, возникнет утечка памяти.</p>
 *
 * <p>Тут пришлось пожертвовать возможностью повторного вхождения в метод.
 * По этой же причине {@link MethodInvocationEvent} не участвует в подсчёте статистики времени выполнения вложенных вызовов.
 * В JFR пишется каждое событие.</p>
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
    void before(JfrJoinPoint joinPoint, E event);

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
