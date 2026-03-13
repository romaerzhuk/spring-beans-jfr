package jfr.logging;

import jfr.api.JoinPointCallback;
import jfr.api.LoggingJoinPoint;
import jfr.event.MethodInvocationEvent;
import jfr.event.NonReentrantMethodEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrLoggingServiceImpl}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class JfrLoggingServiceImplTest implements MethodSourceHelper {
    static class TestEvent extends NonReentrantMethodEvent {
    }

    @Spy
    @InjectMocks
    JfrLoggingServiceImpl<TestEvent> subj;
    @Autowired
    @Mock
    JfrLoggingHelper helper;
    @Autowired
    @Mock
    JfrLoggingContextHolder contextHolder;
    @Autowired
    @Mock
    JfrReentrantLoggingContextStrategy reentrantContextStrategy;
    @Autowired
    @Mock
    JfrNonReentrantLoggingContextStrategy nonReentrantContextStrategy;

    @Test
    void proceed() throws Throwable {
        var joinPoint = mock(ProceedingJoinPoint.class);
        Object result = uidS();
        doReturn(result).when(joinPoint).proceed();
        Object expected = uidS();
        doAnswer(inv -> {
            JoinPointCallback callback = inv.getArgument(1);
            verify(joinPoint, never()).proceed();

            assertThat(callback.proceed()).isEqualTo(result);
            verify(joinPoint).proceed();
            return expected;
        }).when(subj).proceedCallback(eq(LoggingJoinPoint.of(joinPoint)), any());

        Object actual = subj.proceed(joinPoint);

        assertThat(actual).isEqualTo(expected);
        verify(subj).proceed(any());
        verify(subj).proceedCallback(any(), any());
        verify(joinPoint).proceed();
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint);
    }

    @Test
    void proceedCallback_success() throws Throwable {
        var joinPoint = mock(LoggingJoinPoint.class);
        var callback = mock(JoinPointCallback.class);
        var logger = LoggerFactory.getLogger(JfrLoggingServiceImpl.class);
        var context = mock(LoggingContext.class);
        doReturn(context).when(helper).before(eq(joinPoint), eq(reentrantContextStrategy), isA(MethodInvocationEvent.class), eq(logger));
        Object expected = uidS();
        doReturn(expected).when(callback).proceed();

        Object actual = subj.proceedCallback(joinPoint, callback);

        assertThat(actual).isEqualTo(expected);
        var inOrder = inOrder(subj, helper, callback);
        inOrder.verify(subj).proceedCallback(any(), any());
        inOrder.verify(helper).before(any(), any(), any(), any());
        inOrder.verify(callback).proceed();
        inOrder.verify(helper).afterReturning(context, joinPoint, expected);
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint, callback, context);
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void proceedCallback_exception(Throwable thrown) throws Throwable {
        var joinPoint = mock(LoggingJoinPoint.class);
        var callback = mock(JoinPointCallback.class);
        var logger = LoggerFactory.getLogger(JfrLoggingServiceImpl.class);
        var context = mock(LoggingContext.class);
        doReturn(context).when(helper).before(eq(joinPoint), eq(reentrantContextStrategy), isA(MethodInvocationEvent.class), eq(logger));
        doThrow(thrown).when(callback).proceed();

        var t = assertThrows(Throwable.class, () -> subj.proceedCallback(joinPoint, callback));

        assertThat(t).isEqualTo(thrown);
        var inOrder = inOrder(subj, helper, callback);
        inOrder.verify(subj).proceedCallback(any(), any());
        inOrder.verify(helper).before(any(), any(), any(), any());
        inOrder.verify(callback).proceed();
        inOrder.verify(helper).afterThrowing(context, joinPoint, thrown);
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint, callback, context);
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void proceedCallback_contextIsNullSuccess() throws Throwable {
        var joinPoint = mock(LoggingJoinPoint.class);
        var callback = mock(JoinPointCallback.class);
        var logger = LoggerFactory.getLogger(JfrLoggingServiceImpl.class);
        doReturn(null).when(helper).before(eq(joinPoint), eq(reentrantContextStrategy), isA(MethodInvocationEvent.class), eq(logger));
        Object expected = uidS();
        doReturn(expected).when(callback).proceed();

        Object actual = subj.proceedCallback(joinPoint, callback);

        assertThat(actual).isEqualTo(expected);
        var inOrder = inOrder(subj, helper, callback);
        inOrder.verify(subj).proceedCallback(any(), any());
        inOrder.verify(helper).before(any(), any(), any(), any());
        inOrder.verify(callback).proceed();
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint, callback);
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void proceedCallback_contextIsNullExceptions(Throwable thrown) throws Throwable {
        var joinPoint = mock(LoggingJoinPoint.class);
        var callback = mock(JoinPointCallback.class);
        var logger = LoggerFactory.getLogger(JfrLoggingServiceImpl.class);
        doReturn(null).when(helper).before(eq(joinPoint), eq(reentrantContextStrategy), isA(MethodInvocationEvent.class), eq(logger));
        doThrow(thrown).when(callback).proceed();

        var t = assertThrows(Throwable.class, () -> subj.proceedCallback(joinPoint, callback));

        assertThat(t).isEqualTo(thrown);
        var inOrder = inOrder(subj, helper, callback);
        inOrder.verify(subj).proceedCallback(any(), any());
        inOrder.verify(helper).before(any(), any(), any(), any());
        inOrder.verify(callback).proceed();
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint, callback);
    }

    static Stream<Throwable> exceptions() {
        return Stream.of(new RuntimeException(uidS()), new Exception(uidS()), new Error(uidS()), new Throwable(uidS()));
    }

    @Test
    void before() {
        var joinPoint = mock(LoggingJoinPoint.class);
        var event = mock(TestEvent.class);

        subj.before(joinPoint, event);

        verify(subj).before(any(), any());
        verify(helper).before(joinPoint, nonReentrantContextStrategy, event, LoggerFactory.getLogger(JfrLoggingServiceImpl.class));
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, joinPoint, event);
    }

    @Test
    void afterReturning() {
        var context = mock(LoggingContext.class);
        doReturn(context).when(contextHolder).getContext();
        Object retVal = uidS();

        subj.afterReturning(TestEvent.class, retVal);

        verify(subj).afterReturning(any(), any());
        verify(context).afterReturningNonReentrant(TestEvent.class, retVal);
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, context);
    }

    @Test
    void afterReturning_contextIsNull() {
        doReturn(null).when(contextHolder).getContext();
        Object retVal = uidS();

        subj.afterReturning(TestEvent.class, retVal);

        verify(subj).afterReturning(any(), any());
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy);
    }

    @Test
    void afterThrowing() {
        var context = mock(LoggingContext.class);
        doReturn(context).when(contextHolder).getContext();
        var cause = new Throwable(uidS());

        subj.afterThrowing(TestEvent.class, cause);

        verify(subj).afterThrowing(any(), any());
        verify(context).afterThrowingNonReentrant(TestEvent.class, cause);
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy, context);
    }

    @Test
    void afterThrowing_contextIsNull() {
        doReturn(null).when(contextHolder).getContext();
        var cause = new Throwable(uidS());

        subj.afterThrowing(TestEvent.class, cause);

        verify(subj).afterThrowing(any(), any());
        verifyNoMoreInteractions(subj, contextHolder, reentrantContextStrategy, nonReentrantContextStrategy);
    }
}