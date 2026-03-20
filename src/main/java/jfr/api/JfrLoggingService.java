package jfr.api;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Регистрирует в лог и журнал Java Flight Recorder (JFR) статистику времени выполнения бизнес-метода.
 *
 * @author Roman_Erzhukov
 */
public interface JfrLoggingService {
    /**
     * Регистрирует в лог и JFR статистику времени выполнения бизнес-метода.
     *
     * @param joinPoint вызываемая операция
     * @return результат операции
     * @throws Throwable исключение целевой операции
     */
    Object proceed(ProceedingJoinPoint joinPoint) throws Throwable;

    /**
     * Регистрирует в лог и JFR статистику времени выполнения бизнес-метода.
     *
     * @param joinPoint вызываемая операция
     * @param callback  вызывает целевой метод
     * @return результат операции
     * @throws Throwable исключение целевой операции
     */
    Object proceedCallback(JfrJoinPoint joinPoint, JoinPointCallback callback) throws Throwable;
}
