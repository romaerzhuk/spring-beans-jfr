package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.MethodInvocationEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Стек вызовов событий {@link MethodInvocationEvent}, {@link LoggingCallback}.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
class LoggingCallbackStack extends ArrayDeque<LoggingCallback> {
    /**
     * Удаляет вызовы, индекс которых больше или равен {@link JfrJoinPoint#index()}.
     *
     * @param joinPoint точка вызова
     * @param context   контекст регистрации событий
     * @return {@link LoggingCallback}
     */
    @Nullable
    public LoggingCallback removeIfIndexGreaterOrEqual(JfrJoinPoint joinPoint, LoggingContext context) {
        log.trace("removeIfIndexGreaterOrEqual - start {}", joinPoint);
        LoggingCallback last = peekLast();
        int jointPointIndex = joinPoint.index();
        while (true) {
            int index = last == null ? 0 : last.index();
            if (last == null || index < jointPointIndex) {
                log.error("removeIfIndexGreaterOrEqual Workaround. Непредвиденное поведение. Часть статистики JFR потеряна: index={} {}",
                        last == null ? null : index, joinPoint);
                return null;
            }
            removeLast();
            last.endEvent();
            last.stop(context);
            LoggingCallback prev = peekLast();
            if (prev != null) {
                prev.resume();
            } else {
                last.commit(context);
            }
            if (index == jointPointIndex) {
                break;
            }
            log.warn("removeIfIndexGreaterOrEqual Workaround. Непредвиденное поведение: joinPoint={}, last={}", joinPoint, last.joinPoint());
            last.logSuccess(null);
            last = prev;
        }
        log.trace("removeIfIndexGreaterOrEqual - end {}", joinPoint);
        return last;
    }
}
