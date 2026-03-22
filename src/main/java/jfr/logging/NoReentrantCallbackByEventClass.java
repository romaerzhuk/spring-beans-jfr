package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.NonReentrantMethodEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

/**
 * Вызовы событий {@link NonReentrantMethodEvent}, {@link LoggingCallback}.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
class NoReentrantCallbackByEventClass extends HashMap<Class<? extends AbstractMethodEvent>, LoggingCallback> {
    /**
     * Удаляет событие {@link NonReentrantMethodEvent} из коллекции.
     *
     * @param eventClass класс события
     * @param context    контекст регистрации событий
     * @return {@link LoggingCallback} или null
     */
    @Nullable
    LoggingCallback removeEvent(Class<? extends AbstractMethodEvent> eventClass, LoggingContext context) {
        LoggingCallback callback = remove(eventClass);
        if (callback == null) {
            return null;
        }
        LoggingCallbackStack callbacks = context.callbacks();
        if (callback == callbacks.peekLast()) { // штатная ситуация
            removeIfIndexGreaterOrEqual(callback.joinPoint());
            callbacks.removeIfIndexGreaterOrEqual(callback.joinPoint(), context);
        } else {
            log.warn("remove Workaround. Непредвиденное поведение:" +
                    " callback != last, callback={}, last={}", callback, callbacks.peekLast());
            callback.commitEvent();
        }
        return callback;
    }

    /**
     * Удаляет вызовы, индекс которых больше или равен {@link JfrJoinPoint#index()}.
     *
     * <p>Workaround. Штатно каждая точка вызова должна удаляться после завершения выполнения.</p>
     *
     * @param joinPoint точка вызова
     */
    void removeIfIndexGreaterOrEqual(JfrJoinPoint joinPoint) {
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
            callback.commitEvent();
            callback.logSuccess(null);
            return true;
        });
    }
}
