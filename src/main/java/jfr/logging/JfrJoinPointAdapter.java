package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.api.JfrLoggingService;
import org.aspectj.lang.JoinPoint;

import java.util.List;

/**
 * Адаптер {@link JoinPoint} для {@link JfrLoggingService}.
 *
 * @param index       индекс фрейма стеков вызовов
 * @param targetClass целевой класс
 * @param name        краткое имя метода, пишется в лог и JFR
 * @param method      полное имя метода, пишется только в лог
 * @param args        аргументы вызова, пишутся только в лог
 * @author Roman_Erzhukov
 */
record JfrJoinPointAdapter(int index, Class<?> targetClass, Object name, Object method, List<Object> args)
        implements JfrJoinPoint {
}
