package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.core.log.LogMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link LoggingCallback}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings("LoggingSimilarMessage")
public class LoggingCallbackTest implements MethodSourceHelper {
    static class TestClass1 {
    }

    static class TestClass2 {
    }

    LoggingCallback subj;
    @Mock
    JfrJoinPoint joinPoint;
    @Mock
    AbstractMethodEvent event;
    @Mock
    Logger logger;
    @Mock
    Stopwatch stopwatch;

    String name;
    Class<?> targetClass;
    Object method;
    int index;
    List<Object> args;

    @BeforeEach
    void init() {
        subj = newLoggingCallback(false, false, false, false, false);
    }

    LoggingCallback newLoggingCallback(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        name = uidS();
        targetClass = List.of(TestClass1.class, TestClass2.class).get(uid(2));
        method = uidS();
        index = uid();
        args = !hasArgs ? null : List.of(uid(), uidS());
        return new LoggingCallback(joinPoint, eventEnabled ? event : null, loggerEnabled ? logger : null, logErrorEnabled, name, targetClass,
                method, index, hasStopwatch ? stopwatch : null, args);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void beginEvent_enabled(boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        event = mock(MethodInvocationEvent.class);
        subj = newLoggingCallback(true, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.beginEvent();

        assertThat(event).is(nestable("event",
                isEqual("beanClass", event.beanClass, targetClass),
                isEqual("method", event.method, name)
        ));
        verify(event).begin();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void beginEvent_disabled(boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(false, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.beginEvent();

        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void endEvent(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        event = mock(MethodInvocationEvent.class);
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.endEvent();

        verify(event, times(eventEnabled ? 1 : 0)).end();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void commitEvent(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        event = mock(MethodInvocationEvent.class);
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.commitEvent();

        verify(event, times(eventEnabled ? 1 : 0)).commit();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void beginLogger(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.beginLogger();

        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} start {}", name, args);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void logSuccess(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);
        Object retVal = uidS();

        subj.logSuccess(retVal);

        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, hasStopwatch ? stopwatch.toString() : "", retVal);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void logFailure(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);
        String stopwatchStr = hasStopwatch ? stopwatch.toString() : "";
        var thrown = new Throwable(uidS());

        subj.logFailure(thrown);

        verify(logger, times(loggerEnabled && logErrorEnabled ? 1 : 0)).error("{} end {}: {}", name, args, stopwatchStr, thrown);
        verify(logger, times(loggerEnabled && !logErrorEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, stopwatchStr, thrown.toString());
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void suspend(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.suspend();

        verify(stopwatch, times(hasStopwatch ? 1 : 0)).stop();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void resume(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, hasStopwatch, hasArgs);

        subj.resume();

        verify(stopwatch, times(hasStopwatch ? 1 : 0)).start();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @MethodSource
    @ParameterizedTest
    void stop(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        var event = mock(MethodInvocationEvent.class);
        this.event = event;
        subj = newLoggingCallback(true, loggerEnabled, logErrorEnabled, true, hasArgs);
        var context = mock(LoggingContext.class);
        long nanos = uid();
        doReturn(nanos).when(stopwatch).elapsed(TimeUnit.NANOSECONDS);
        var statistics = mock(LoggingStatistic.class);
        doReturn(statistics).when(context).getStatistic(targetClass, method);
        doAnswer(inv -> {
            assertThat(event.max).isEqualTo(nanos);
            return null;
        }).when(statistics).update(stopwatch, event);

        subj.stop(context);

        var inOrder = inOrder(stopwatch, logger, statistics);
        inOrder.verify(stopwatch).stop();
        inOrder.verify(stopwatch).elapsed(any());
        inOrder.verify(statistics).update(stopwatch, event);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch, context, statistics);
    }

    static Stream<Arguments> stop() {
        return MethodSourceHelper.join(
                MethodSourceHelper.booleans3(),
                Stream.of(1, 2));
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void stop_MethodInvocationEvent(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, true, hasArgs);
        var context = mock(LoggingContext.class);
        var statistics = mock(LoggingStatistic.class);
        doReturn(statistics).when(context).getStatistic(targetClass, method);

        subj.stop(context);

        var inOrder = inOrder(stopwatch, logger, statistics);
        inOrder.verify(stopwatch).stop();
        inOrder.verify(statistics).update(stopwatch, null);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch, context, statistics);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void stop_disabled(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled, false, hasArgs);
        var context = mock(LoggingContext.class);

        subj.stop(context);

        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch, context);
    }

    @MethodSource
    @ParameterizedTest
    void commit_loggerEnabled(boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs, Boolean methodInvocationEvent) {
        var event = mock(MethodInvocationEvent.class);
        this.event = isTrue(methodInvocationEvent) ? event : this.event;
        subj = newLoggingCallback(methodInvocationEvent != null, true, logErrorEnabled, hasStopwatch, hasArgs);
        var context = mock(LoggingContext.class);
        var logMessage = mock(LogMessage.class);
        doReturn(logMessage).when(context).toStatistics();

        subj.commit(context);

        verify(logger).debug("{} {} {} statistics: {}", targetClass.getSimpleName(), method, args, logMessage);
        verify(context, times(isTrue(methodInvocationEvent) ? 1 : 0)).commit(event);
        verifyNoMoreInteractions(joinPoint, this.event, logger, stopwatch, event, context);
    }

    static Stream<Arguments> commit_loggerEnabled() {
        return MethodSourceHelper.join(
                MethodSourceHelper.booleans3(),
                MethodSourceHelper.booleansWithNull());
    }

    @MethodSource
    @ParameterizedTest
    void commit_loggerDisabled(boolean logErrorEnabled, boolean hasStopwatch, boolean hasArgs, Boolean methodInvocationEvent) {
        var event = mock(MethodInvocationEvent.class);
        this.event = isTrue(methodInvocationEvent) ? event : this.event;
        subj = newLoggingCallback(methodInvocationEvent != null, false, logErrorEnabled, hasStopwatch, hasArgs);
        var context = mock(LoggingContext.class);

        subj.commit(context);

        verify(context, times(isTrue(methodInvocationEvent) ? 1 : 0)).commit(event);
        verifyNoMoreInteractions(joinPoint, this.event, logger, stopwatch, event, context);
    }

    static Stream<Arguments> commit_loggerDisabled() {
        return MethodSourceHelper.join(
                MethodSourceHelper.booleans3(),
                MethodSourceHelper.booleansWithNull());
    }
}