package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * Вызовы событий {@link NonReentrantMethodEvent}, {@link LoggingCallback}.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
public class NoReentrantCallbackByEventClass extends HashMap<Class<? extends AbstractMethodEvent>, LoggingCallback> {
    /**
     * Удаляет вызовы, индекс которых больше или равен {@link JfrJoinPoint#index()}.
     *
     * <p>Workaround. Штатно каждая точка вызова должна удаляться после завершения выполнения.</p>
     *
     * @param joinPoint точка вызова
     */
    public void removeIfIndexGreaterOrEqual(JfrJoinPoint joinPoint) {
        log.trace("removeIfIndexGreaterOrEqual {}", joinPoint);
        int index = joinPoint.index();
        entrySet().removeIf(entry -> {
            LoggingCallback callback = entry.getValue();
            Class<?> eventClass = entry.getKey();
            if (callback.index() < index) {
                log.trace("removeIfIndexGreaterOrEqual {} {} {}: index < {} => false", joinPoint, eventClass, callback, index);
                return false;
            }
            log.warn("removeIfIndexGreaterOrEqual Workaround. Непредвиденное поведение {} {} {}: index >= {} => true",
                    joinPoint, eventClass, callback, index);
            callback.endEvent();
            callback.logSuccess(null);
            return true;
        });
    }
}
