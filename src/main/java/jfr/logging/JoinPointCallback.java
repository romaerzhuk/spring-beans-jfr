package jfr.logging;

/**
 * Вызывает целевой метод.
 *
 * @author Roman_Erzhukov
 */
public interface JoinPointCallback {
    /**
     * Вызывает целевой метод.
     */
    Object proceed() throws Throwable;
}
