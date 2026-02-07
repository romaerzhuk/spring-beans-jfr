package jfr.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;

/**
 * Точка вызова для {@link JfrLoggingService}.
 *
 * @author Roman_Erzhukov
 */
public interface LoggingJoinPoint {
    /**
     * Возвращает уникальный экземпляр выполняемого вызова.
     *
     * <p>Используется для проверки нарушений вызовов {@link JfrLoggingService#proceed(ProceedingJoinPoint)},
     * {@link JfrLoggingService#proceedCallback(LoggingJoinPoint, JoinPointCallback)}.
     * </p>
     */
    Object identityPoint();

    /**
     * Целевой клас компонента Spring.
     */
    Class<?> targetClass();

    /**
     * Возвращает краткое имя метода, пишется в лог и JFR.
     */
    Object name();

    /**
     * Возвращает полное описание метода, пишется только в лог.
     */
    Object method();

    /**
     * Аргументы метода, пишутся только в лог.
     */
    List<Object> args();

    /**
     * Возвращает {@link LoggingJoinPoint} для AspectJ {@link JoinPoint}.
     *
     * @param joinPoint точка вызова
     */
    static LoggingJoinPoint of(JoinPoint joinPoint) {
        return new AspectLoggingJoinPoint(joinPoint);
    }

    /**
     * Возвращает адаптер {@link LoggingJoinPoint}.
     *
     * @param targetClass целевой класс
     * @param name        краткое имя метода, пишется в лог и JFR
     * @param method      полное имя метода, пишется только в лог
     * @param args        аргументы вызова, пишутся только в лог
     */
    static LoggingJoinPoint of(Class<?> targetClass, Object name, Object method, List<Object> args) {
        return of(null, targetClass, name, method, args);
    }

    /**
     * Возвращает адаптер {@link LoggingJoinPoint}.
     *
     * @param identityPoint уникальный экземпляр выполняемого вызова, или null, тогда используется экземпляр LoggingJoinPoint
     * @param targetClass   целевой класс
     * @param name          краткое имя метода, пишется в лог и JFR
     * @param method        полная сигнатура метода, пишется только в лог
     * @param args          аргументы вызова, пишутся только в лог
     */
    static LoggingJoinPoint of(Object identityPoint, Class<?> targetClass, Object name, Object method, List<Object> args) {
        return new LoggingJoinPointAdapter(identityPoint, targetClass, name, method, args);
    }
}
