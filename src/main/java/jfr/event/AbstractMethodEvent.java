package jfr.event;

import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Событие вызова метода Spring-бина.
 *
 * @author Roman_Erzhukov
 */
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
}
