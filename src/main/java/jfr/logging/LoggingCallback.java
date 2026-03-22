package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Вызывается для регистрации в лог и журнал Java Flight Recorder статистики времени выполнения бизнес-метода.
 *
 * @param joinPoint       точка вызова
 * @param event           событие JFR, или null
 * @param logger          логгер, или null
 * @param logErrorEnabled признак принудительной записи в лог стектрейса исключений
 * @param name            имя
 * @param targetClass     целевой класс
 * @param method          вызываемый метод
 * @param index           индекс фрейма стека вызова
 * @param stopwatch       засекает время выполнения, или null
 * @param args            аргументы вызова для записи в лог, или null
 * @author Roman_Erzhukov
 */
@Slf4j
@SuppressWarnings("LoggingSimilarMessage")
record LoggingCallback(
        JfrJoinPoint joinPoint,
        AbstractMethodEvent event,
        @Nullable
        Logger logger,
        boolean logErrorEnabled,
        String name,
        Class<?> targetClass,
        Object method,
        int index,
        Stopwatch stopwatch,
        @Nullable
        List<Object> args) {

    /**
     * Начинает событие JFR.
     */
    public void beginEvent() {
        log.trace("beginEvent {}", event);
        event.beanClass = targetClass;
        event.method = name;
        event.begin();
    }

    /**
     * Завершает подсчёт времени выполнения события JFR.
     */
    public void endEvent() {
        log.trace("endEvent {}", event);
        event.end();
    }

    /**
     * Пишет в лог начало вызова.
     */
    public void beginLogger() {
        if (logger != null) {
            logger.debug("{} start {}", name, args);
        }
    }

    /**
     * Пишет в лог результат успешного выполнения.
     *
     * @param retVal результат выполнения
     */
    public void logSuccess(Object retVal) {
        if (logger != null) {
            logger.debug("{} end {}: {} {}", name, args, stopwatch, retVal);
        }
    }

    /**
     * Пишет в лог ошибку выполнения.
     *
     * @param thrown ошибка
     */
    public void logFailure(Throwable thrown) {
        if (logger == null) {
            return;
        }
        if (logErrorEnabled) {
            logger.error("{} end {}: {}", name, args, stopwatch, thrown);
        } else {
            logger.debug("{} end {}: {} {}", name, args, stopwatch, thrown.toString());
        }
    }

    /**
     * Останавливает stopwatch.
     */
    public void suspend() {
        stopwatch.stop();
    }

    /**
     * Запускает stopwatch
     */
    public void resume() {
        stopwatch.start();
    }

    /**
     * Завершает подсчёт времени выполнения.
     *
     * @param context контекст
     */
    public void stop(LoggingContext context) {
        stopwatch.stop();
        event.max = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        context.getStatistic(targetClass, method)
                .update(stopwatch, event);
    }

    /**
     * Фиксирует статистику в лог и/или JFR.
     *
     * @param context контекст
     */
    public void commit(LoggingContext context) {
        if (logger != null) {
            logger.debug("{} {} {} statistics: {}", targetClass.getSimpleName(), method, args, context.toStatistics());
        }
        context.commit(event);
    }
}
