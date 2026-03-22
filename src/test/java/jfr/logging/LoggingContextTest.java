package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import jfr.logging.LoggingContext.Key;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.log.LogMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.match;
import static jfr.test.hamcrest.PropertiesMatcher.matching;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Тесты для {@link LoggingContext}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings("ResultOfMethodCallIgnored")
public class LoggingContextTest implements MethodSourceHelper {
    static class TestEvent extends NonReentrantMethodEvent {
    }

    LoggingContext subj;
    @Mock
    Logger logger;
    @Mock
    LoggingCallbackStack callbacks;
    @Mock
    Map<Key, LoggingStatistic> statistics;
    @Mock
    NoReentrantCallbackByEventClass noReentrantCallbackByClass;

    int pointValue;
    long thresholdNanos;

    @BeforeEach
    void setUp() {
        pointValue = uid();
        thresholdNanos = uid();
        subj = mock(LoggingContext.class, withSettings()
                .name("subj")
                .defaultAnswer(CALLS_REAL_METHODS)
                .useConstructor(logger, thresholdNanos, statistics, callbacks, noReentrantCallbackByClass));
    }

    @Test
    void constructor() {
        subj = new LoggingContext(thresholdNanos);

        assertThat(subj).is(nestable("LoggingContext",
                isEqual("logger", subj.logger(), LoggerFactory.getLogger(LoggingContext.class)),
                isEqual("thresholdNanos", subj.thresholdNanos(), thresholdNanos),
                match("statistics", subj.statistics(), anEmptyMap()),
                match("callbacks", subj.callbacks(), empty()),
                match("noReentrantCallbackByClass", subj.noReentrantCallbackByClass(), allOf(instanceOf(HashMap.class), anEmptyMap()))
        ));
    }

    @Test
    void before_withPrev() {
        var prev = mock(LoggingCallback.class, "prev");
        doReturn(prev).when(callbacks).peekLast();
        var callback = mock(LoggingCallback.class, "callback");

        subj.before(callback);

        var inOrder = inOrder(callbacks, callback, prev);
        inOrder.verify(callbacks).peekLast();
        inOrder.verify(callbacks).add(callback);
        inOrder.verify(prev).suspend();
        inOrder.verify(callback).resume();
        inOrder.verify(callback).beginEvent();
        inOrder.verify(callback).beginLogger();
        verifyNoMoreInteractions(logger, statistics, callbacks, noReentrantCallbackByClass, callback, prev);
    }

    @Test
    void before_withoutPrev() {
        var callback = mock(LoggingCallback.class);

        subj.before(callback);

        var inOrder = inOrder(subj, callbacks, callback);
        inOrder.verify(subj).before(any());
        inOrder.verify(callbacks).peekLast();
        inOrder.verify(callbacks).add(callback);
        inOrder.verify(callback).resume();
        inOrder.verify(callback).beginEvent();
        inOrder.verify(callback).beginLogger();
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, callback);
    }

    @Test
    void beforeNonReentrant() {
        doNothing().when(subj).before(any());
        var callback = mock(LoggingCallback.class);
        var event = new TestEvent();

        subj.beforeNonReentrant(callback, event);

        var inOrder = inOrder(subj, noReentrantCallbackByClass, callback);
        inOrder.verify(subj).beforeNonReentrant(any(), any());
        inOrder.verify(noReentrantCallbackByClass).put(event.getClass(), callback);
        inOrder.verify(subj).before(callback);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, callback);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void afterReturning(int index) {
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbacks).removeIfIndexGreaterOrEqual(joinPoint, subj);
        Object retVal = uidS();

        boolean actual = subj.afterReturning(joinPoint, retVal);

        assertThat(actual).isEqualTo(index == 0);
        verify(subj).afterReturning(any(), any());
        verify(noReentrantCallbackByClass).removeIfIndexGreaterOrEqual(joinPoint);
        verify(callback).logSuccess(retVal);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, joinPoint, callback);
    }

    @Test
    void afterReturning_null() {
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(null).when(callbacks).removeIfIndexGreaterOrEqual(joinPoint, subj);
        Object retVal = uidS();

        boolean actual = subj.afterReturning(joinPoint, retVal);

        assertThat(actual).isTrue();
        verify(subj).afterReturning(any(), any());
        verify(noReentrantCallbackByClass).removeIfIndexGreaterOrEqual(joinPoint);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, joinPoint);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterReturningNonReentrant(boolean hasCallback) {
        var callback = mock(LoggingCallback.class);
        doReturn(hasCallback ? callback : null).when(noReentrantCallbackByClass).removeEvent(TestEvent.class, subj);
        Object retVal = uidS();

        subj.afterReturningNonReentrant(TestEvent.class, retVal);

        verify(subj).afterReturningNonReentrant(any(), any());
        verify(callback, times(hasCallback ? 1 : 0)).logSuccess(retVal);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, callback);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void afterThrowing(int index) {
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbacks).removeIfIndexGreaterOrEqual(joinPoint, subj);
        var cause = new Throwable(uidS());

        boolean actual = subj.afterThrowing(joinPoint, cause);

        assertThat(actual).isEqualTo(index == 0);
        verify(subj).afterThrowing(any(), any());
        verify(noReentrantCallbackByClass).removeIfIndexGreaterOrEqual(joinPoint);
        verify(callback).logFailure(cause);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, joinPoint, callback);
    }

    @Test
    void afterThrowing_null() {
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(null).when(callbacks).removeIfIndexGreaterOrEqual(joinPoint, subj);
        var cause = new Throwable(uidS());

        boolean actual = subj.afterThrowing(joinPoint, cause);

        assertThat(actual).isTrue();
        verify(subj).afterThrowing(any(), any());
        verify(noReentrantCallbackByClass).removeIfIndexGreaterOrEqual(joinPoint);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, joinPoint);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterThrowingNonReentrant(boolean hasCallback) {
        var callback = mock(LoggingCallback.class);
        doReturn(hasCallback ? callback : null).when(noReentrantCallbackByClass).removeEvent(TestEvent.class, subj);
        var cause = new Throwable(uidS());

        subj.afterThrowingNonReentrant(TestEvent.class, cause);

        verify(subj).afterThrowingNonReentrant(any(), any());
        verify(callback, times(hasCallback ? 1 : 0)).logFailure(cause);
        verifyNoMoreInteractions(subj, logger, statistics, callbacks, noReentrantCallbackByClass, callback);
    }

    @Test
    void getStatistic() {
        class TestClass1 {
        }
        class TestClass2 {
        }
        String name = uidS();
        var expected = mock(LoggingStatistic.class);
        doAnswer(inv -> {
            Function<Key, LoggingStatistic> callback = inv.getArgument(1);

            LoggingStatistic actual = callback.apply(Key.of(TestClass1.class, uidS()));

            assertThat(actual).is(nestable("LoggingStatistic",
                    isEqual("count", actual.getCount(), 0),
                    isEqual("sum", actual.getSum(), 0L),
                    isEqual("min", actual.getMin(), Long.MAX_VALUE),
                    isEqual("max", actual.getMax(), Long.MIN_VALUE)
            ));
            return expected;
        }).when(statistics).computeIfAbsent(eq(Key.of(TestClass2.class, name)), any());

        LoggingStatistic actual = subj.getStatistic(TestClass2.class, name);

        assertThat(actual).isEqualTo(expected);
    }

    @MethodSource
    @ParameterizedTest
    void commit_enabled(int offset) {
        var event = mock(MethodInvocationEvent.class, "event");
        long max = event.max = thresholdNanos + offset;
        MethodInvocationEvent[] events = Stream.concat(Stream.of(event), Stream.generate(() -> {
                    var e = mock(MethodInvocationEvent.class, "event" + uid());
                    e.max = uid();
                    return e;
                })).limit(3 + uid(5)) // 3..7
                .toArray(MethodInvocationEvent[]::new);
        LoggingStatistic[] statistics = Stream.of(events)
                .map(e -> {
                    var s = mock(LoggingStatistic.class, "stat" + uid());
                    doReturn(e).when(s).getEvent();
                    return s;
                }).toArray(LoggingStatistic[]::new);
        doReturn(List.of(statistics)).when(this.statistics).values();
        doAnswer(inv -> {
            assertThat(event).is(matching(matcher -> matcher
                    .add("count", event.count, 1)
                    .add("min", event.min, max)
                    .add("max", event.max, max)
                    .add("sum", event.sum, max)
                    .add("avg", event.avg, max)
            ));
            return null;
        }).when(event).commit();
        Object[] mocks = Stream.of(Stream.of(events), Stream.of(statistics), Stream.of(event)).flatMap(Function.identity()).toArray();

        subj.commit(event);

        verify(event).commit();
        IntStream.range(0, statistics.length)
                .forEach(i -> {
                    verify(statistics[i]).getEvent();
                    verify(statistics[i], times(i != 0 ? 1 : 0)).commit();
                    verify(events[i], times(i == 0 ? 1 : 0)).commit();
                });
        verifyNoMoreInteractions(mocks);
    }

    static Stream<Integer> commit_enabled() {
        return Stream.of(0, 1);
    }

    @MethodSource
    @ParameterizedTest
    void commit_disabled(int offset) {
        var event = mock(MethodInvocationEvent.class, "event");
        event.max = thresholdNanos + offset;
        MethodInvocationEvent[] events = Stream.concat(Stream.of(event), Stream.generate(() -> {
                    var e = mock(MethodInvocationEvent.class, "event" + uid());
                    e.max = uid();
                    return e;
                })).limit(3 + uid(5)) // 3..7
                .toArray(MethodInvocationEvent[]::new);
        Object[] mocks = Stream.concat(Stream.of(events), Stream.of(event)).toArray();

        subj.commit(event);

        verifyNoInteractions(mocks);
    }

    static Stream<Integer> commit_disabled() {
        return Stream.of(-2, -1);
    }

    @Test
    void toStatistics() {
        class TestClass1 {
        }
        class TestClass2 {
        }
        int minSum = uid() * 1000;
        int avgSum = uid() * 1000;
        int maxSum = uid() * 1000;
        Answer<Void> appendAnswer = inv -> {
            StringBuilder sb = inv.getArgument(0);
            Object method = inv.getArgument(2);
            sb.append(method).append(" ");
            return null;
        };
        var stat1 = spy(newLoggingStatistic().setSum(minSum));
        var method1 = uid();
        doAnswer(appendAnswer).when(stat1).appendTo(any(), eq(TestClass1.class), eq(method1));
        var stat2 = spy(newLoggingStatistic().setSum(maxSum));
        var method2 = uid();
        doAnswer(appendAnswer).when(stat2).appendTo(any(), eq(TestClass2.class), eq(method2));
        var stat3 = spy(newLoggingStatistic().setSum(avgSum));
        var method3 = uid();
        doAnswer(appendAnswer).when(stat3).appendTo(any(), eq(TestClass1.class), eq(method3));
        doReturn(Map.of(
                Key.of(TestClass1.class, method1), stat1,
                Key.of(TestClass2.class, method2), stat2,
                Key.of(TestClass1.class, method3), stat3).entrySet())
                .when(statistics).entrySet();

        LogMessage actual = subj.toStatistics();

        assertThat(actual.toString()).isEqualTo(DurationFormatUtils.formatDurationHMS((minSum + avgSum + maxSum) / 1000_000)
                + String.format("%s %s %s ", method2, method3, method1));
    }

    LoggingStatistic newLoggingStatistic() {
        return new LoggingStatistic().setCount(uid());
    }
}