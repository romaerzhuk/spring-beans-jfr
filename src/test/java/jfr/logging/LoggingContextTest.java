package jfr.logging;

import com.google.common.base.Ticker;
import jfr.event.AbstractMethodEvent;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import jfr.logging.LoggingContext.Key;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jfr.test.hamcrest.PropertiesMatcher.matching;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Тесты для {@link LoggingContext}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings("unchecked")
public class LoggingContextTest implements MethodSourceHelper {
    static class TestEvent1 extends NonReentrantMethodEvent implements Predicate<LoggingJoinPoint> {
        @Override
        public boolean test(LoggingJoinPoint joinPoint) {
            throw new UnsupportedOperationException();
        }
    }

    static class TestEvent2 extends NonReentrantMethodEvent {
    }

    static class TestEvent3 extends NonReentrantMethodEvent {
    }

    static class TestEvent4 extends NonReentrantMethodEvent {
    }

    LoggingContext subj;

    @Mock
    Logger logger;

    int pointValue;
    Object identityPoint;
    long thresholdNanos;

    @BeforeEach
    void setUp() {
        pointValue = uid();
        identityPoint = String.valueOf(pointValue);
        thresholdNanos = uid();
        subj = mock(LoggingContext.class, withSettings()
                .name("subj")
                .defaultAnswer(CALLS_REAL_METHODS)
                .useConstructor(identityPoint, logger, thresholdNanos));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void constructor(boolean hasIdentityPoint) {
        var joinPoint = mock(LoggingJoinPoint.class);
        doReturn(hasIdentityPoint ? identityPoint : null).when(joinPoint).identityPoint();

        subj = new LoggingContext(joinPoint, thresholdNanos);

        assertThat(subj).is(matching(matcher -> matcher
                .add("identityPoint", subj.identityPoint, hasIdentityPoint ? identityPoint : joinPoint)
                .add("logger", subj.logger, LoggerFactory.getLogger(LoggingContext.class))
                .add("thresholdNanos", subj.thresholdNanos, thresholdNanos)
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void before(boolean hasPrev) {
        var prev = mock(LoggingCallback.class, "prev");
        var callback = mock(LoggingCallback.class, "callback");
        subj.callback = hasPrev ? prev : null;
        var ticker = mock(Ticker.class);

        subj.before(callback, ticker);

        assertThat(subj.callback).isEqualTo(callback);
        verify(callback).before(hasPrev ? prev : null, ticker);
        verifyNoMoreInteractions(logger, callback, prev, ticker);
    }

    @ParameterizedTest
    @MethodSource("booleans2")
    void beforeNonReentrant(boolean hasEventClass, boolean hasPredicate) {
        var prev = mock(LoggingCallback.class, "prev");
        subj.callback = prev;
        var callback = mock(LoggingCallback.class, "callback");
        var other = mock(LoggingCallback.class, "other");
        var event1 = new TestEvent1();
        var event = hasPredicate ? event1 : new TestEvent2();
        subj.callbackByNoReentrantEventClass.put(hasEventClass ? event.getClass() : TestEvent3.class, other);
        Predicate<LoggingJoinPoint> predicate1 = mock(Predicate.class, "predicate1");
        Predicate<LoggingJoinPoint> predicate2 = mock(Predicate.class, "predicate2");
        subj.predicateByNoReentrantEventClass.putAll(
                Map.of(event.getClass(), predicate1, TestEvent4.class, predicate2));
        Map<Class<? extends AbstractMethodEvent>, LoggingCallback> callbackByNoReentrantEventClass = hasEventClass
                ? Map.of(event.getClass(), callback) : Map.of(event.getClass(), callback, TestEvent3.class, other);
        Map<Class<? extends AbstractMethodEvent>, Predicate<LoggingJoinPoint>> predicateByNoReentrantEventClass =
                Map.of(event.getClass(), hasPredicate ? event1 : predicate1, TestEvent4.class, predicate2);

        subj.beforeNonReentrant(callback, event);

        assertThat(subj).is(matching(matcher -> matcher
                .add("callbackByNoReentrantEventClass", subj.callbackByNoReentrantEventClass, callbackByNoReentrantEventClass)
                .add("predicateByNoReentrantEventClass", subj.predicateByNoReentrantEventClass, predicateByNoReentrantEventClass)
        ));
        verify(callback).before(null, null);
        verifyNoMoreInteractions(callback, prev, predicate1, predicate2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterReturning(boolean expected) {
        var callback = subj.callback = mock(LoggingCallback.class, "callback");
        var joinPoint = mock(LoggingJoinPoint.class);
        Object retVal = uidS();
        var prev = mock(LoggingCallback.class, "prev");
        doReturn(prev).when(callback).afterReturning(subj, retVal);
        doReturn(expected).when(subj).after(prev, joinPoint);

        boolean actual = subj.afterReturning(joinPoint, retVal);

        assertThat(actual).isEqualTo(expected);
        verify(callback).afterReturning(subj, retVal);
        verifyNoMoreInteractions(callback, joinPoint, prev);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterReturningNonReentrant(boolean hasCallback) {
        var callback = mock(LoggingCallback.class, "callback");
        var other = mock(LoggingCallback.class, "other");
        subj.callbackByNoReentrantEventClass.putAll(Map.of(TestEvent1.class, other, hasCallback ? TestEvent2.class : TestEvent3.class, callback));
        Predicate<LoggingJoinPoint> predicate1 = mock(Predicate.class, "predicate1");
        Predicate<LoggingJoinPoint> predicate2 = mock(Predicate.class, "predicate2");
        subj.predicateByNoReentrantEventClass.putAll(Map.of(
                TestEvent1.class, predicate1, hasCallback ? TestEvent2.class : TestEvent3.class, predicate2));
        Map<Class<? extends AbstractMethodEvent>, LoggingCallback> callbackByNoReentrantEventClass = hasCallback
                ? Map.of(TestEvent1.class, other) : Map.of(TestEvent1.class, other, TestEvent3.class, callback);
        Map<Class<? extends AbstractMethodEvent>, Predicate<LoggingJoinPoint>> predicateByNoReentrantEventClass = hasCallback
                ? Map.of(TestEvent1.class, predicate1) : Map.of(TestEvent1.class, predicate1, TestEvent3.class, predicate2);
        Object retVal = uidS();

        subj.afterReturningNonReentrant(TestEvent2.class, retVal);

        assertThat(subj).is(matching(matcher -> matcher
                .add("callbackByNoReentrantEventClass", subj.callbackByNoReentrantEventClass, callbackByNoReentrantEventClass)
                .add("predicateByNoReentrantEventClass", subj.predicateByNoReentrantEventClass, predicateByNoReentrantEventClass)
        ));
        verify(callback, times(hasCallback ? 1 : 0)).afterReturning(subj, retVal);
        verifyNoMoreInteractions(callback, predicate1, predicate2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterThrowing(boolean expected) {
        var callback = subj.callback = mock(LoggingCallback.class, "callback");
        var joinPoint = mock(LoggingJoinPoint.class);
        var cause = new Throwable(uidS());
        var prev = mock(LoggingCallback.class, "prev");
        doReturn(prev).when(callback).afterThrowing(subj, cause);
        doReturn(expected).when(subj).after(prev, joinPoint);

        boolean actual = subj.afterThrowing(joinPoint, cause);

        assertThat(actual).isEqualTo(expected);
        verify(callback).afterThrowing(subj, cause);
        verifyNoMoreInteractions(callback, prev);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void afterThrowingNonReentrant(boolean hasCallback) {
        var callback = mock(LoggingCallback.class, "callback");
        var other = mock(LoggingCallback.class, "other");
        subj.callbackByNoReentrantEventClass.putAll(Map.of(TestEvent1.class, other, hasCallback ? TestEvent2.class : TestEvent3.class, callback));
        Predicate<LoggingJoinPoint> predicate1 = mock(Predicate.class, "predicate1");
        Predicate<LoggingJoinPoint> predicate2 = mock(Predicate.class, "predicate2");
        subj.predicateByNoReentrantEventClass.putAll(Map.of(
                TestEvent1.class, predicate1, hasCallback ? TestEvent2.class : TestEvent3.class, predicate2));
        Map<Class<? extends AbstractMethodEvent>, LoggingCallback> callbackByNoReentrantEventClass = hasCallback
                ? Map.of(TestEvent1.class, other) : Map.of(TestEvent1.class, other, TestEvent3.class, callback);
        Map<Class<? extends AbstractMethodEvent>, Predicate<LoggingJoinPoint>> predicateByNoReentrantEventClass = hasCallback
                ? Map.of(TestEvent1.class, predicate1) : Map.of(TestEvent1.class, predicate1, TestEvent3.class, predicate2);
        var cause = new Throwable(uidS());

        subj.afterThrowingNonReentrant(TestEvent2.class, cause);

        assertThat(subj).is(matching(matcher -> matcher
                .add("callbackByNoReentrantEventClass", subj.callbackByNoReentrantEventClass, callbackByNoReentrantEventClass)
                .add("predicateByNoReentrantEventClass", subj.predicateByNoReentrantEventClass, predicateByNoReentrantEventClass)
        ));
        verify(callback, times(hasCallback ? 1 : 0)).afterThrowing(subj, cause);
        verifyNoMoreInteractions(callback, predicate1, predicate2);
    }

    @ParameterizedTest
    @MethodSource("booleans2")
    void after(boolean hasCallback, boolean same) {
        var callback = mock(LoggingCallback.class);
        var other = mock(LoggingJoinPoint.class);
        var point = same ? identityPoint : String.valueOf(pointValue);
        lenient().doReturn(point).when(other).identityPoint();
        lenient().doNothing().when(subj).tryAfterNoReentrant(any());

        boolean actual = subj.after(hasCallback ? callback : null, other);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(!hasCallback || same);
            s.assertThat(subj.callback).as("callback").isEqualTo(hasCallback ? callback : null);
        });
        verify(subj).after(any(), any());
        verify(subj).tryAfterNoReentrant(other);
        verify(logger, times(!hasCallback || !same ? 0 : 1)).error("При вызове {} не все вложенные операции завершились." +
                " Не соблюдается соответствие вызовов before/afterReturning или before/afterThrowing." +
                " Часть статистики JFR потеряна", other);
        verify(other, times(hasCallback ? 1 : 0)).identityPoint();
        verifyNoMoreInteractions(subj, logger, other);
    }

    @Test
    void tryAfterNoReentrant() {
        var joinPoint = mock(LoggingJoinPoint.class);
        List<Predicate<LoggingJoinPoint>> predicates = Stream.generate(() -> (Predicate<LoggingJoinPoint>) mock(Predicate.class, "predicate" + uid()))
                .limit(3).toList();
        subj.predicateByNoReentrantEventClass.putAll(
                Map.of(TestEvent1.class, predicates.get(0), TestEvent2.class, predicates.get(1), TestEvent3.class, predicates.get(2)));
        LoggingCallback[] callback = Stream.generate(() -> mock(LoggingCallback.class, "callback" + uid())).limit(3).toArray(LoggingCallback[]::new);
        subj.callbackByNoReentrantEventClass.putAll(
                Map.of(TestEvent1.class, callback[0], TestEvent3.class, callback[1], TestEvent4.class, callback[2]));
        doReturn(false).when(predicates.get(0)).test(joinPoint);
        doReturn(true).when(predicates.get(1)).test(joinPoint);
        doReturn(true).when(predicates.get(2)).test(joinPoint);

        subj.tryAfterNoReentrant(joinPoint);

        assertThat(subj).is(matching(matcher -> matcher
                .add("predicateByNoReentrantEventClass", subj.predicateByNoReentrantEventClass, Map.of(TestEvent1.class, predicates.get(0)))
                .add("callbackByNoReentrantEventClass", subj.callbackByNoReentrantEventClass,
                        Map.of(TestEvent1.class, callback[0], TestEvent4.class, callback[2]))
        ));
        verify(subj).tryAfterNoReentrant(any());
        verify(callback[1]).afterReturning(subj, null);
        verifyNoMoreInteractions(subj, callback[0], callback[1], callback[2]);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getIdentityPoint(boolean hasIdentityPoint) {
        var joinPoint = mock(LoggingJoinPoint.class);
        Object expected = hasIdentityPoint ? uidS() : joinPoint;
        doReturn(hasIdentityPoint ? expected : null).when(joinPoint).identityPoint();

        Object actual = subj.getIdentityPoint(joinPoint);

        assertThat(actual).isSameAs(expected);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getStatistic(boolean found) {
        class TestClass1 {
        }
        class TestClass2 {
        }
        String name = uidS();
        int count = uid();
        long sum = uid();
        long min = uid();
        long max = uid();
        var statistic = newLoggingStatistic()
                .setCount(count)
                .setSum(sum)
                .setMin(min)
                .setMax(max);
        Map<Key, LoggingStatistic> origin = Map.of(Key.of(TestClass1.class, name), newLoggingStatistic(),
                Key.of(TestClass2.class, found ? name : uidS()), statistic);
        subj.statistics.putAll(origin);

        LoggingStatistic actual = subj.getStatistic(TestClass2.class, name);

        Map<? extends Key, LoggingStatistic> expectedStatistics = found ? origin
                : Stream.concat(origin.entrySet().stream(), Stream.of(Pair.of(Key.of(TestClass2.class, name), actual)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(actual).is(matching(matcher -> matcher
                .add("this", actual, found ? sameInstance(statistic) : not(sameInstance(statistic)))
                .add("count", actual.getCount(), found ? count : 0)
                .add("sum", actual.getSum(), found ? sum : 0)
                .add("min", actual.getMin(), found ? min : Long.MAX_VALUE)
                .add("max", actual.getMax(), found ? max : Long.MIN_VALUE)
                .add("statistics", subj.statistics, expectedStatistics)
        ));
    }

    @MethodSource
    @ParameterizedTest
    void commit(int offset) {
        var event = mock(MethodInvocationEvent.class, "event");
        long max = event.max = thresholdNanos + offset;
        MethodInvocationEvent[] events = Stream.concat(Stream.of(event), Stream.generate(() -> {
                    var e = mock(MethodInvocationEvent.class, "event" + uid());
                    e.max = uid();
                    return e;
                })).limit(3 + uid(5))
                .toArray(MethodInvocationEvent[]::new);
        LoggingStatistic[] statistics = Stream.of(events)
                .map(e -> {
                    var s = mock(LoggingStatistic.class, "stat" + uid());
                    lenient().doReturn(e).when(s).getEvent();
                    return s;
                }).toArray(LoggingStatistic[]::new);
        subj.statistics.putAll(Stream.of(statistics)
                .collect(Collectors.toMap(s -> Key.of(getClass(), uid()), Function.identity())));
        lenient().doAnswer(inv -> {
            assertThat(event).is(matching(matcher -> matcher
                    .add("count", event.count, 1)
                    .add("min", event.min, max)
                    .add("max", event.max, max)
                    .add("sum", event.sum, max)
                    .add("avg", event.avg, max)
            ));
            return null;
        }).when(event).commit();
        boolean expected = offset >= 0;

        subj.commit(event);

        IntStream.range(0, statistics.length)
                .forEach(i -> {
                    verify(statistics[i], times(expected ? 1 : 0)).getEvent();
                    verify(statistics[i], times(expected && i != 0 ? 1 : 0)).commit();
                    verify(events[i], times(expected && i == 0 ? 1 : 0)).commit();
                });
        verifyNoMoreInteractions(Stream.of(events, statistics).flatMap(Stream::of).toArray());
    }

    static Stream<Integer> commit() {
        return Stream.of(-1, 0, 1);
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
        subj.statistics.putAll(Map.of(
                Key.of(TestClass1.class, method1), stat1,
                Key.of(TestClass2.class, method2), stat2,
                Key.of(TestClass1.class, method3), stat3));

        LogMessage actual = subj.toStatistics();

        assertThat(actual.toString()).isEqualTo(DurationFormatUtils.formatDurationHMS((minSum + avgSum + maxSum) / 1000_000)
                + String.format("%s %s %s ", method2, method3, method1));
    }

    LoggingStatistic newLoggingStatistic() {
        return new LoggingStatistic().setCount(uid());
    }
}