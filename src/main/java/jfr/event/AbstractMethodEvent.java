package jfr.event;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jfr.api.JfrJoinPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * Событие вызова метода Spring-бина.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMethodEvent extends Event {
    /**
     * Класс бина.
     */
    @Label("class")
    public Class<?> beanClass;

    /**
     * Вызываемый метод.
     */
    public String method;

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{beanClass=" + (beanClass == null ? "null" : beanClass.getSimpleName()) + ", method=" + method + '}';
    }


    /**
     * Возвращает true, если включена запись в JFR или в лог, иначе false.
     *
     * @param joinPoint точка вызова
     * @param logger    логгер
     * @return true, если включена запись в JFR или в лог, иначе false
     */
    public boolean isEnabled(JfrJoinPoint joinPoint, Logger logger) {
        boolean eventEnabled = isEnabled();
        boolean debugEnabled = logger.isDebugEnabled();
        boolean result = eventEnabled || debugEnabled;
        log.trace("isEnabled {} {} eventEnabled={}, debugEnabled={} => {}", joinPoint, this, eventEnabled, debugEnabled, result);
        return result;
    }
}
