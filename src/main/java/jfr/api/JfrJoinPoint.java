package jfr.api;

import java.util.List;

/**
 * Точка вызова для {@link JfrLoggingService}.
 *
 * @author Roman_Erzhukov
 */
public interface JfrJoinPoint {
    /**
     * Индекс фрейма стека вызовов.
     *
     * @return индекс
     */
    int index();

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
}
