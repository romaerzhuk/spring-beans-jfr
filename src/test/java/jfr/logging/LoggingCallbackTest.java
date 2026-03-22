package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.api.JfrJoinPoint;
import jfr.event.MethodInvocationEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.core.log.LogMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
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
    MethodInvocationEvent event;
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
        subj = newLoggingCallback(false, false, false);
    }

    LoggingCallback newLoggingCallback(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        name = uidS();
        targetClass = List.of(TestClass1.class, TestClass2.class).get(uid(2));
        method = uidS();
        index = uid();
        args = !hasArgs ? null : List.of(uid(), uidS());
        return new LoggingCallback(joinPoint, event, loggerEnabled ? logger : null, logErrorEnabled,
                name, targetClass, method, index, stopwatch, args);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void beginEvent(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);

        subj.beginEvent();

        assertThat(event).is(nestable("event",
                isEqual("beanClass", event.beanClass, targetClass),
                isEqual("method", event.method, name)
        ));
        verify(event).begin();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void endEvent(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);

        subj.endEvent();

        verify(event).end();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void beginLogger(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);

        subj.beginLogger();

        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} start {}", name, args);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void logSuccess(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);
        Object retVal = uidS();

        subj.logSuccess(retVal);

        verify(logger, times(loggerEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, stopwatch, retVal);
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void logFailure(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);
        var thrown = new Throwable(uidS());

        subj.logFailure(thrown);

        verify(logger, times(loggerEnabled && logErrorEnabled ? 1 : 0)).error("{} end {}: {}", name, args, stopwatch, thrown);
        verify(logger, times(loggerEnabled && !logErrorEnabled ? 1 : 0)).debug("{} end {}: {} {}", name, args, stopwatch, thrown.toString());
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void suspend(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);

        subj.suspend();

        verify(stopwatch).stop();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void resume(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);

        subj.resume();

        verify(stopwatch).start();
        verifyNoMoreInteractions(joinPoint, event, logger, stopwatch);
    }

    @ParameterizedTest
    @MethodSource("booleans3")
    void stop(boolean loggerEnabled, boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(loggerEnabled, logErrorEnabled, hasArgs);
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

    @ParameterizedTest
    @MethodSource("booleans2")
    void commit_loggerEnabled(boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(true, logErrorEnabled, hasArgs);
        var context = mock(LoggingContext.class);
        var logMessage = mock(LogMessage.class);
        doReturn(logMessage).when(context).toStatistics();

        subj.commit(context);

        verify(logger).debug("{} {} {} statistics: {}", targetClass.getSimpleName(), method, args, logMessage);
        verify(context).commit(event);
        verifyNoMoreInteractions(joinPoint, this.event, logger, stopwatch, event, context);
    }

    @ParameterizedTest
    @MethodSource("booleans2")
    void commit_loggerDisabled(boolean logErrorEnabled, boolean hasArgs) {
        subj = newLoggingCallback(false, logErrorEnabled, hasArgs);
        var context = mock(LoggingContext.class);

        subj.commit(context);

        verify(context).commit(event);
        verifyNoMoreInteractions(joinPoint, this.event, logger, stopwatch, event, context);
    }
}