package jfr.logging;

import org.aspectj.lang.JoinPoint;

import java.util.List;

/**
 * Адаптер {@link JoinPoint} для {@link JfrLoggingService}.
 *
 * @param identityPoint уникальный экземпляр выполняемого вызова, или null, тогда используется экземпляр LoggingJoinPoint
 * @param targetClass   целевой класс
 * @param name          краткое имя метода, пишется в лог и JFR
 * @param method        полное имя метода, пишется только в лог
 * @param args          аргументы вызова, пишутся только в лог
 * @author Roman_Erzhukov
 */
record LoggingJoinPointAdapter(Object identityPoint, Class<?> targetClass, Object name, Object method, List<Object> args)
        implements LoggingJoinPoint {
}
