package jfr.logging;

import com.google.common.base.Ticker;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.stream.Stream;

import static jfr.test.hamcrest.PropertiesMatcher.matching;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrLoggingServiceImpl}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class JfrLoggingServiceImplTest implements MethodSourceHelper {
    static class TestEventClass extends NonReentrantMethodEvent {
    }

    @Spy
    @InjectMocks
    JfrLoggingServiceImpl<TestEventClass> subj;

    @Mock
    Ticker ticker;

    @Mock
    Function<Class<?>, Logger> loggerFactory;

    @AfterEach
    void reset() {
        JfrLoggingServiceImpl.context.remove();
    }

    @ParameterizedTest
    @MethodSource("proceedArguments")
    void proceed(boolean hasContext, Throwable thrown) throws Throwable {
        var context = mock(LoggingContext.class);
        doReturn(hasContext ? context : null).when(subj).doBefore(any(), anyBoolean(), any(), any());
        var joinPoint = mock(ProceedingJoinPoint.class);
        Object expected = uidS();
        doAnswer(inv -> {
            if (thrown != null) {
                throw thrown;
            }
            return expected;
        }).when(joinPoint).proceed();
        lenient().doNothing().when(subj).doAfterReturning(any(), any(), any());
        lenient().doNothing().when(subj).doAfterThrowing(any(), any(), any());
        var loggingJoinPoint = LoggingJoinPoint.of(joinPoint);

        if (thrown == null) {
            Object actual = subj.proceed(joinPoint);
            assertThat(actual).isEqualTo(expected);
        } else {
            var t = assertThrows(Throwable.class, () -> subj.proceed(joinPoint));
            assertThat(t).isSameAs(thrown);
        }
        var inOrder = inOrder(subj, joinPoint);
        inOrder.verify(subj).proceed(any());
        inOrder.verify(subj).doBefore(eq(loggingJoinPoint), eq(true),
                isA(MethodInvocationEvent.class), eq(LoggerFactory.getLogger(JfrLoggingServiceImpl.class)));
        inOrder.verify(subj, times(hasContext && thrown == null ? 1 : 0)).doAfterReturning(context, loggingJoinPoint, expected);
        inOrder.verify(subj, times(hasContext && thrown != null ? 1 : 0)).doAfterThrowing(context, loggingJoinPoint, thrown);
        verifyNoMoreInteractions(subj, joinPoint, context);
    }

    @ParameterizedTest
    @MethodSource("proceedArguments")
    void proceedCallback(boolean hasContext, Throwable thrown) throws Throwable {
        var context = mock(LoggingContext.class);
        doReturn(hasContext ? context : null).when(subj).doBefore(any(), anyBoolean(), any(), any());
        Object expected = uidS();
        var callback = mock(JoinPointCallback.class);
        doAnswer(inv -> {
            if (thrown != null) {
                throw thrown;
            }
            return expected;
        }).when(callback).proceed();
        lenient().doNothing().when(subj).doAfterReturning(any(), any(), any());
        lenient().doNothing().when(subj).doAfterThrowing(any(), any(), any());
        var joinPoint = mock(LoggingJoinPoint.class);

        if (thrown == null) {
            Object actual = subj.proceedCallback(joinPoint, callback);
            assertThat(actual).isEqualTo(expected);
        } else {
            var t = assertThrows(Throwable.class, () -> subj.proceedCallback(joinPoint, callback));
            assertThat(t).isSameAs(thrown);
        }
        var inOrder = inOrder(subj, joinPoint);
        inOrder.verify(subj).proceedCallback(any(), any());
        inOrder.verify(subj).doBefore(eq(joinPoint), eq(true),
                isA(MethodInvocationEvent.class), eq(LoggerFactory.getLogger(JfrLoggingServiceImpl.class)));
        inOrder.verify(subj, times(hasContext && thrown == null ? 1 : 0)).doAfterReturning(context, joinPoint, expected);
        inOrder.verify(subj, times(hasContext && thrown != null ? 1 : 0)).doAfterThrowing(context, joinPoint, thrown);
        verifyNoMoreInteractions(subj, joinPoint, context);
    }

    static Stream<Arguments> proceedArguments() {
        return MethodSourceHelper.join(
                MethodSourceHelper.booleans(),
                Stream.of(null, new RuntimeException(uidS()), new Exception(uidS()), new Error(uidS()), new Throwable(uidS())));
    }

    @Test
    void before() {
        var context = mock(LoggingContext.class);
        doReturn(context).when(subj).doBefore(any(), anyBoolean(), any(), any());
        var joinPoint = mock(LoggingJoinPoint.class);
        var event = mock(TestEventClass.class);

        subj.before(joinPoint, event);

        verify(subj).before(any(), any());
        verify(subj).doBefore(joinPoint, false, event, LoggerFactory.getLogger(JfrLoggingServiceImpl.class));
        verifyNoMoreInteractions(subj, context, joinPoint);
    }

    @ParameterizedTest
    @MethodSource("booleans5")
    void doBefore(boolean eventEnabled, boolean debugEnabled, boolean logErrorEnabled, boolean hasContext, boolean methodInvocationEvent) {
        subj.logErrorEnabled = logErrorEnabled;
        lenient().doNothing().when(subj).removeContext();
        var event = mock(TestEventClass.class);
        doReturn(eventEnabled).when(event).isEnabled();
        var log = mock(Logger.class, "log");
        doReturn(debugEnabled).when(log).isDebugEnabled();
        var joinPoint = mock(LoggingJoinPoint.class);
        var context = mock(LoggingContext.class);
        doReturn(hasContext ? context : null).when(subj).getContext();
        lenient().doReturn(context).when(subj).createContext(any());
        Class<?> targetClass = getClass();
        lenient().doReturn(targetClass).when(joinPoint).targetClass();
        var logger = mock(Logger.class, "logger");
        lenient().doReturn(logger).when(loggerFactory).apply(any());
        String name = uidS();
        lenient().doReturn(name).when(joinPoint).name();
        Object method = uidS();
        lenient().doReturn(method).when(joinPoint).method();
        Answer<Void> answer = inv -> {
            LoggingCallback callback = inv.getArgument(0);
            verify(subj, never()).setContext(any());
            assertThat(callback).is(matching(matcher -> matcher
                    .add("joinPoint", callback.joinPoint, joinPoint)
                    .add("event", callback.event, eventEnabled ? event : null)
                    .add("logger", callback.logger, debugEnabled ? logger : null)
                    .add("logErrorEnabled", callback.logErrorEnabled, logErrorEnabled)
                    .add("name", callback.name, name)
                    .add("targetClass", callback.targetClass, targetClass)
                    .add("method", callback.method, method)
            ));
            return null;
        };
        lenient().doAnswer(answer).when(context).before(any(), any());
        lenient().doAnswer(answer).when(context).beforeNonReentrant(any(), any());
        lenient().doNothing().when(subj).setContext(any());
        boolean expected = (debugEnabled || eventEnabled) && (hasContext || methodInvocationEvent);

        LoggingContext actual = subj.doBefore(joinPoint, methodInvocationEvent, event, log);

        assertThat(actual).as("actual").isEqualTo(expected ? context : null);
        verify(subj).doBefore(any(), anyBoolean(), any(), any());
        verify(subj, times(expected ? 0 : 1)).removeContext();
        verify(subj, times(expected && !hasContext ? 1 : 0)).createContext(joinPoint);
        verify(joinPoint, times(expected ? 1 : 0)).targetClass();
        verify(loggerFactory, times(expected && debugEnabled ? 1 : 0)).apply(targetClass);
        verify(joinPoint, times(expected ? 1 : 0)).name();
        verify(joinPoint, times(expected ? 1 : 0)).method();
        verify(context, times(expected && methodInvocationEvent ? 1 : 0)).before(isA(LoggingCallback.class), eq(ticker));
        verify(context, times(expected && !methodInvocationEvent ? 1 : 0)).beforeNonReentrant(isA(LoggingCallback.class), eq(event));
        verify(subj, times(expected && !hasContext ? 1 : 0)).setContext(context);
        verifyNoMoreInteractions(subj, loggerFactory, event, log, context, joinPoint, logger);
    }

    @ParameterizedTest
    @MethodSource("booleans")
    void createContext(boolean hasIdentityPoint) {
        var joinPoint = mock(LoggingJoinPoint.class);
        Object identityPoint = uidS();
        doReturn(hasIdentityPoint ? identityPoint : null).when(joinPoint).identityPoint();
        long threshold = subj.thresholdNanos = uid();

        LoggingContext actual = subj.createContext(joinPoint);

        assertThat(actual).is(matching(matcher -> matcher
                .add("identityPoint", actual.identityPoint, hasIdentityPoint ? identityPoint : joinPoint)
                .add("logger", actual.logger, LoggerFactory.getLogger(LoggingContext.class))
                .add("thresholdNanos", actual.thresholdNanos, threshold)
        ));
    }

    @ParameterizedTest
    @MethodSource("booleans")
    void afterReturning(boolean hasContext) {
        var context = mock(LoggingContext.class);
        doReturn(hasContext ? context : null).when(subj).getContext();
        Object retVal = uidS();

        subj.afterReturning(TestEventClass.class, retVal);

        verify(context, times(hasContext ? 1 : 0)).afterReturningNonReentrant(TestEventClass.class, retVal);
        verifyNoMoreInteractions(context);
    }

    @ParameterizedTest
    @MethodSource("booleans")
    void doAfterReturning(boolean expected) {
        lenient().doNothing().when(subj).removeContext();
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        Object retVal = uidS();
        doReturn(expected).when(context).afterReturning(joinPoint, retVal);

        subj.doAfterReturning(context, joinPoint, retVal);

        verify(subj).doAfterReturning(any(), any(), any());
        verify(subj, times(expected ? 1 : 0)).removeContext();
        verifyNoMoreInteractions(subj, context);
    }

    @ParameterizedTest
    @MethodSource("booleans")
    void afterThrowing(boolean hasContext) {
        var context = mock(LoggingContext.class);
        doReturn(hasContext ? context : null).when(subj).getContext();
        var cause = new Throwable(uidS());

        subj.afterThrowing(TestEventClass.class, cause);

        verify(context, times(hasContext ? 1 : 0)).afterThrowingNonReentrant(TestEventClass.class, cause);
        verifyNoMoreInteractions(context);
    }

    @ParameterizedTest
    @MethodSource("booleans")
    void doAfterThrowing(boolean expected) {
        lenient().doNothing().when(subj).removeContext();
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        var cause = new Throwable(uidS());
        doReturn(expected).when(context).afterThrowing(joinPoint, cause);

        subj.doAfterThrowing(context, joinPoint, cause);

        verify(subj).doAfterThrowing(any(), any(), any());
        verify(subj, times(expected ? 1 : 0)).removeContext();
        verifyNoMoreInteractions(subj, context);
    }

    @Test
    void getContext() {
        var expected = mock(LoggingContext.class);
        JfrLoggingServiceImpl.context.set(expected);

        LoggingContext actual = subj.getContext();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setContext() {
        var value = mock(LoggingContext.class);

        subj.setContext(value);

        assertThat(JfrLoggingServiceImpl.context.get()).isEqualTo(value);
    }

    @Test
    void removeContext() {
        JfrLoggingServiceImpl.context.set(mock(LoggingContext.class));

        subj.removeContext();

        assertThat(JfrLoggingServiceImpl.context.get()).isNull();
    }
}