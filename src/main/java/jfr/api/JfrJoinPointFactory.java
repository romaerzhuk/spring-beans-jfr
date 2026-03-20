package jfr.api;

import java.util.List;

/**
 * Создаёт точку вызова {@link JfrJoinPoint}.
 *
 * @author Roman_Erzhukov
 */
public interface JfrJoinPointFactory {
    /**
     * Создаёт {@link JfrJoinPoint}.
     *
     * @param targetClass целевой класс
     * @param name        краткое имя метода, пишется в лог и JFR
     * @param method      полное имя метода, пишется только в лог
     * @param args        аргументы вызова, пишутся только в лог
     */
    JfrJoinPoint create(Class<?> targetClass, Object name, Object method, List<Object> args);
}
