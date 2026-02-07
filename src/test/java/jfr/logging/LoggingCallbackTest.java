package jfr.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.core.log.LogMessage;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidL;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Тесты для {@link LoggingCallback}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings({"ResultOfMethodCallIgnored", "LoggingSimilarMessage"})
public class LoggingCallbackTest implements MethodSourceHelper {
    static class TestClass {
    }

    static class TestClass2 {
    }

    LoggingCallback subj;
    @Mock
    LoggingJoinPoint joinPoint;
    @Mock
    AbstractMethodEvent event;
    @Mock
    Logger logger;

    Class<?> testClass;
    String name;
    Object method;

    @BeforeEach
    void init() {
        subj = newLoggingCallback(false, false, false);
    }

    LoggingCallback newLoggingCallback(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled) {
        name = uidS();
        method = uidS();
        testClass = List.of(TestClass.class, TestClass2.class, getClass()).get(uid(3));
        return mock(LoggingCallback.class, withSettings()
                .name("subj")
                .defaultAnswer(CALLS_REAL_METHODS)
                .useConstructor(joinPoint,
                        eventEnabled ? event : null,
                        loggerEnabled ? logger : null,
                        logErrorEnabled, name, testClass, method));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void before(boolean hasTicker) {
        lenient().doNothing().when(subj).start(any(), any());
        doNothing().when(subj).beginEvent();
        doNothing().when(subj).beginLogger();
        var prev = mock(LoggingCallback.class);
        var ticker = mock(Ticker.class);

        subj.before(prev, hasTicker ? ticker : null);

        var inOrder = inOrder(subj);
        inOrder.verify(subj).before(any(), any());
        inOrder.verify(subj, times(hasTicker ? 1 : 0)).start(prev, ticker);
        inOrder.verify(subj).beginEvent();
        inOrder.verify(subj).beginLogger();
        verifyNoMoreInteractions(subj, prev, ticker);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void afterReturning(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopWatch) {
        var event = mock(MethodInvocationEvent.class);
        this.event = event;
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        var stopWatch = mock(Stopwatch.class);
        subj.stopwatch = hasStopWatch ? stopWatch : null;
        lenient().doNothing().when(subj).stop(any(), any());
        doNothing().when(subj).logSuccess(any());
        var afterResult = mock(LoggingCallback.class);
        lenient().doReturn(afterResult).when(subj).after(any(), any());
        var context = mock(LoggingContext.class);
        Object retVal = uidS();

        LoggingCallback actual = subj.afterReturning(context, retVal);

        assertThat(actual).isEqualTo(hasStopWatch ? afterResult : null);
        var inOrder = inOrder(subj, event);
        inOrder.verify(subj).afterReturning(any(), any());
        inOrder.verify(subj, times(hasStopWatch ? 1 : 0)).stop(context, eventEnabled ? event : null);
        inOrder.verify(event, times(eventEnabled && !hasStopWatch ? 1 : 0)).commit();
        inOrder.verify(subj).logSuccess(retVal);
        inOrder.verify(subj, times(hasStopWatch ? 1 : 0)).after(context, eventEnabled ? event : null);
        verifyNoMoreInteractions(subj, event, afterResult, context);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void logSuccess(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        List<Object> args = subj.args = List.of(uid(), uidS());
        var stopwatch = mock(Stopwatch.class);
        subj.stopwatch = hasStopwatch ? stopwatch : null;
        Object stopwatchStr = hasStopwatch ? stopwatch : "";
        Object retVal = uidS();

        subj.logSuccess(retVal);

        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, stopwatchStr, retVal);
        verifyNoMoreInteractions(logger);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void afterThrowing(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopWatch) {
        var event = mock(MethodInvocationEvent.class);
        this.event = event;
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        var stopwatch = mock(Stopwatch.class);
        subj.stopwatch = hasStopWatch ? stopwatch : null;
        lenient().doNothing().when(subj).stop(any(), any());
        var context = mock(LoggingContext.class);
        var thrown = new Throwable(uidS());
        var afterResult = mock(LoggingCallback.class);
        lenient().doReturn(afterResult).when(subj).after(any(), any());
        doNothing().when(subj).logFailure(any());

        LoggingCallback actual = subj.afterThrowing(context, thrown);

        assertThat(actual).isEqualTo(hasStopWatch ? afterResult : null);
        var inOrder = inOrder(subj, event);
        inOrder.verify(subj).afterThrowing(any(), any());
        inOrder.verify(subj, times(hasStopWatch ? 1 : 0)).stop(context, eventEnabled ? event : null);
        inOrder.verify(event, times(eventEnabled && !hasStopWatch ? 1 : 0)).end();
        inOrder.verify(subj).logFailure(thrown);
        inOrder.verify(subj, times(hasStopWatch ? 1 : 0)).after(context, eventEnabled ? event : null);
        verifyNoMoreInteractions(subj, event, context, afterResult);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void after(boolean hasPrev) {
        var prev = mock(LoggingCallback.class);
        subj.prev = hasPrev ? prev : null;
        doNothing().when(subj).endEvent(any());
        lenient().doNothing().when(subj).collectStatistic(any(), any());
        var context = mock(LoggingContext.class);
        var event = mock(MethodInvocationEvent.class);

        LoggingCallback actual = subj.after(context, event);

        assertThat(actual).isEqualTo(hasPrev ? prev : null);
        var inOrder = inOrder(subj);
        inOrder.verify(subj).after(any(), any());
        inOrder.verify(subj).endEvent(event);
        inOrder.verify(subj, times(hasPrev ? 0 : 1)).collectStatistic(context, event);
        verifyNoMoreInteractions(subj, prev, context, event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void start(boolean hasPrev) {
        long start = uid();
        long elapsed = uid();
        var ticker = mock(Ticker.class);
        doReturn(start, start + elapsed).when(ticker).read();
        var prev = mock(LoggingCallback.class);

        subj.start(hasPrev ? prev : null, ticker);

        var inOrder = inOrder(prev, ticker);
        inOrder.verify(prev, times(hasPrev ? 1 : 0)).suspend();
        inOrder.verify(ticker).read();
        verifyNoMoreInteractions(prev, ticker);
        assertThat(subj.stopwatch).is(nestable("state",
                isEqual("subj.prev", subj.prev, hasPrev ? prev : null),
                isEqual("isRunning", subj.stopwatch.isRunning(), true),
                isEqual("elapsed", subj.stopwatch.elapsed(), Duration.ofNanos(elapsed))
        ));
        inOrder.verify(ticker).read();
        verifyNoMoreInteractions(prev, ticker);
    }

    @Test
    void suspend() {
        var stopwatch = subj.stopwatch = mock(Stopwatch.class);

        subj.suspend();

        verify(stopwatch).stop();
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void beginEvent(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled) {
        this.event = mock(MethodInvocationEvent.class);
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        lenient().doAnswer(inv -> {
            assertThat(event).is(nestable("event",
                    isEqual("beanClass", event.beanClass, testClass),
                    isEqual("method", event.method, name)
            ));
            return null;
        }).when(event).begin();

        subj.beginEvent();

        verify(event, times(eventEnabled ? 1 : 0)).begin();
        verifyNoMoreInteractions(event);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void beginLogger(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        List<Object> origin = List.of(uidS(), uid());
        subj.args = origin;
        List<Object> args = List.of(uidS(), uidL());
        lenient().doReturn(args).when(joinPoint).args();

        subj.beginLogger();

        assertThat(subj.args).containsExactlyElementsOf(loggerEnabled ? args : origin);
        verify(joinPoint, times(loggerEnabled ? 1 : 0)).args();
        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} start {}", name, args);
        verifyNoMoreInteractions(joinPoint, logger);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void stop(boolean hasStopwatch, boolean running, boolean hasPrev) {
        var event = mock(MethodInvocationEvent.class);
        var stopwatch = mock(Stopwatch.class);
        lenient().doReturn(running).when(stopwatch).isRunning();
        var prev = mock(LoggingCallback.class);
        subj.stopwatch = hasStopwatch ? stopwatch : null;
        subj.prev = hasPrev ? prev : null;
        var context = mock(LoggingContext.class);
        var statistic = mock(LoggingStatistic.class);
        lenient().doReturn(statistic).when(context).getStatistic(any(), any());

        subj.stop(context, event);

        var inOrder = inOrder(stopwatch, prev, context, statistic);
        inOrder.verify(stopwatch, times(hasStopwatch ? 1 : 0)).isRunning();
        inOrder.verify(stopwatch, times(hasStopwatch && running ? 1 : 0)).stop();
        inOrder.verify(prev, times(hasStopwatch && running && hasPrev ? 1 : 0)).resume();
        inOrder.verify(context, times(hasStopwatch && running ? 1 : 0)).getStatistic(testClass, method);
        inOrder.verify(statistic, times(hasStopwatch && running ? 1 : 0)).update(stopwatch, event);
        verifyNoMoreInteractions(event, context, stopwatch, prev, statistic);
    }

    @Test
    void resume() {
        var stopwatch = subj.stopwatch = mock(Stopwatch.class);

        subj.resume();

        verify(stopwatch).start();
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void logFailure(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasStopwatch) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        List<?> args = subj.args = List.of(uidS(), uid());
        var stopwatch = mock(Stopwatch.class);
        subj.stopwatch = hasStopwatch ? stopwatch : null;
        Object stopwatchStr = hasStopwatch ? stopwatch : "";
        var thrown = new Throwable(uidS());

        subj.logFailure(thrown);

        verify(logger, times(loggerEnabled && logErrorEnabled ? 1 : 0)).error("{} end {}: {}", name, args, stopwatchStr, thrown);
        verify(logger, times(loggerEnabled && !logErrorEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, stopwatchStr, thrown.toString());
        verifyNoMoreInteractions(logger, stopwatch);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void endEvent(boolean hasEvent) {
        var stopWatch = subj.stopwatch = mock(Stopwatch.class);
        long elapsed = uid();
        lenient().doReturn(elapsed).when(stopWatch).elapsed(any());
        var event = mock(MethodInvocationEvent.class);
        lenient().doAnswer(inv -> {
            assertThat(event.max).isEqualTo(elapsed);
            return null;
        }).when(event).end();

        subj.endEvent(hasEvent ? event : null);

        verify(stopWatch, times(hasEvent ? 1 : 0)).elapsed(TimeUnit.NANOSECONDS);
        verify(event, times(hasEvent ? 1 : 0)).end();
        verifyNoMoreInteractions(stopWatch, event);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void collectStatistic(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasEvent) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        List<?> args = subj.args = List.of(uidS(), uid());
        var context = mock(LoggingContext.class);
        var event = mock(MethodInvocationEvent.class);
        var statistics = LogMessage.of(UidExtension::uidS);
        lenient().doReturn(statistics).when(context).toStatistics();

        subj.collectStatistic(context, hasEvent ? event : null);

        var inOrder = inOrder(logger, context);
        inOrder.verify(context, times(loggerEnabled ? 1 : 0)).toStatistics();
        inOrder.verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} {} {} statistics: {}", testClass.getSimpleName(), method, args, statistics);
        inOrder.verify(context, times(hasEvent ? 1 : 0)).commit(event);
        verifyNoMoreInteractions(logger, context, event);
    }

    @ParameterizedTest
    @MethodSource("booleans4")
    void testToString(boolean eventEnabled, boolean loggerEnabled, boolean logErrorEnabled, boolean hasPrev) {
        subj = newLoggingCallback(eventEnabled, loggerEnabled, logErrorEnabled);
        event = eventEnabled ? event : null;
        var prevJoinPoint = mock(LoggingJoinPoint.class, "prevJoinPoint");
        var prevEvent = mock(AbstractMethodEvent.class, "prevEvent");
        var prevLogger = mock(Logger.class, "prevLogger");
        String prevName = uidS();
        Class<?> prevTestClass = testClass == TestClass.class ? TestClass2.class : TestClass.class;
        Object prevMethod = uidS();
        var prev = new LoggingCallback(prevJoinPoint, prevEvent, prevLogger, logErrorEnabled, prevName, prevTestClass, prevMethod);
        subj.prev = hasPrev ? prev : null;
        String expected = LoggingCallback.class.getSimpleName() +
                "{joinPoint=" + joinPoint +
                ", event=" + event +
                ", name=" + name +
                ", targetClass=" + testClass.getSimpleName() +
                ", prev=";
        if (!hasPrev) {
            expected += "null";
        } else {
            expected += LoggingCallback.class.getSimpleName() +
                    "{joinPoint=" + prevJoinPoint +
                    ", event=" + prevEvent +
                    ", name=" + prevName +
                    ", targetClass=" + prevTestClass.getSimpleName() +
                    '}';
        }
        expected += '}';

        String actual = subj.toString();

        assertThat(actual).isEqualTo(expected);
    }
}