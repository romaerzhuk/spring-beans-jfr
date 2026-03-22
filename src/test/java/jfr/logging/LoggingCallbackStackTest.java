package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static jfr.test.junit.UidExtension.uid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link LoggingCallbackStack}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings("ResultOfMethodCallIgnored")
class LoggingCallbackStackTest {
    static class TestEvent extends AbstractMethodEvent {
    }

    static class TestEvent2 extends TestEvent {
    }

    @Spy
    LoggingCallbackStack subj;

    @Test
    void removeByEventClass() {
        var context = mock(LoggingContext.class);
        var callback = newLoggingCallback();
        doReturn(callback).when(subj).peekLast();
        doReturn(new TestEvent()).when(callback).event();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(joinPoint).when(callback).joinPoint();
        var expected = newLoggingCallback();
        doReturn(expected).when(subj).removeIfIndexGreaterOrEqual(joinPoint, context);

        LoggingCallback actual = subj.removeByEventClass(TestEvent.class, context);

        assertThat(actual).isEqualTo(expected);
        verify(subj).removeByEventClass(any(), any());
        verifyNoMoreInteractions(subj, context, callback, joinPoint, expected);
    }

    @MethodSource
    @ParameterizedTest
    void removeByEventClass_unexpected(AbstractMethodEvent event, Class<? extends AbstractMethodEvent> eventClass) {
        var context = mock(LoggingContext.class);
        var callback = newLoggingCallback();
        doReturn(callback).when(subj).peekLast();
        doReturn(event).when(callback).event();

        LoggingCallback actual = subj.removeByEventClass(eventClass, context);

        assertThat(actual).isNull();
        verify(subj).removeByEventClass(any(), any());
        verifyNoMoreInteractions(subj, context, callback);
    }

    static Stream<Arguments> removeByEventClass_unexpected() {
        return Stream.of(
                arguments(new TestEvent(), TestEvent2.class),
                arguments(new TestEvent2(), TestEvent.class));
    }

    @Test
    void removeByEventClass_null() {
        var context = mock(LoggingContext.class);
        doReturn(null).when(subj).peekLast();

        LoggingCallback actual = subj.removeByEventClass(TestEvent.class, context);

        assertThat(actual).isNull();
        verify(subj).removeByEventClass(any(), any());
        verifyNoMoreInteractions(subj, context);
    }

    @Test
    void removeIfIndexGreaterOrEqual() {
        LoggingCallback[] callbacks = {newLoggingCallback(), newLoggingCallback(), newLoggingCallback()};
        subj.addAll(List.of(callbacks));
        int index = uid();
        doReturn(index).when(callbacks[2]).index();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(callbacks[2]);
            s.assertThat(subj).as("subj").containsExactly(callbacks[0], callbacks[1]);
        });
        var inOrder = inOrder(joinPoint, callbacks[1], callbacks[2]);
        inOrder.verify(joinPoint).index();
        inOrder.verify(callbacks[2]).index();
        inOrder.verify(callbacks[2]).endEvent();
        inOrder.verify(callbacks[2]).stop(context);
        inOrder.verify(callbacks[1]).resume();
        verifyNoMoreInteractions(joinPoint, callbacks[0], callbacks[1], callbacks[2]);
    }

    @Test
    void removeIfIndexGreaterOrEqual_commit_single() {
        var callback = newLoggingCallback();
        subj.add(callback);
        int index = uid();
        doReturn(index).when(callback).index();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(callback);
            s.assertThat(subj).as("subj").isEmpty();
        });
        var inOrder = inOrder(joinPoint, callback);
        inOrder.verify(joinPoint).index();
        inOrder.verify(callback).index();
        inOrder.verify(callback).endEvent();
        inOrder.verify(callback).stop(context);
        inOrder.verify(callback).commit(context);
        verifyNoMoreInteractions(joinPoint, callback);
    }

    @Test
    void removeIfIndexGreaterOrEqual_empty() {
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(uid()).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isNull();
            s.assertThat(subj).as("subj").isEmpty();
        });
        verifyNoMoreInteractions(joinPoint);
    }

    @Test
    void removeIfIndexGreaterOrEqual_unexpectedLess() {
        LoggingCallback[] callbacks = {newLoggingCallback(), newLoggingCallback()};
        subj.addAll(List.of(callbacks));
        int index = uid();
        doReturn(index - 1).when(callbacks[1]).index();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(null);
            s.assertThat(subj).as("subj").containsExactly(callbacks);
        });
        verifyNoMoreInteractions(joinPoint, callbacks[0], callbacks[1]);
    }

    @Test
    void removeIfIndexGreaterOrEqual_unexpectedGreater() {
        LoggingCallback[] callbacks = {newLoggingCallback(), newLoggingCallback(), newLoggingCallback(), newLoggingCallback()};
        subj.addAll(List.of(callbacks));
        int index = uid();
        doReturn(index + 2).when(callbacks[3]).index();
        doReturn(index + 1).when(callbacks[2]).index();
        doReturn(index).when(callbacks[1]).index();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(callbacks[1]);
            s.assertThat(subj).as("subj").containsExactly(callbacks[0]);
        });
        var inOrder = inOrder(joinPoint, callbacks[0], callbacks[1], callbacks[2], callbacks[3]);
        inOrder.verify(joinPoint).index();
        inOrder.verify(callbacks[3]).index();
        inOrder.verify(callbacks[3]).endEvent();
        inOrder.verify(callbacks[3]).stop(context);
        inOrder.verify(callbacks[2]).resume();
        inOrder.verify(callbacks[3]).joinPoint();
        inOrder.verify(callbacks[3]).logSuccess(null);
        inOrder.verify(callbacks[2]).index();
        inOrder.verify(callbacks[2]).endEvent();
        inOrder.verify(callbacks[2]).stop(context);
        inOrder.verify(callbacks[1]).resume();
        inOrder.verify(callbacks[2]).joinPoint();
        inOrder.verify(callbacks[2]).logSuccess(null);
        inOrder.verify(callbacks[1]).index();
        inOrder.verify(callbacks[1]).endEvent();
        inOrder.verify(callbacks[1]).stop(context);
        inOrder.verify(callbacks[0]).resume();
        verifyNoMoreInteractions(joinPoint, callbacks[0], callbacks[1], callbacks[2], callbacks[3]);
    }

    @Test
    void removeIfIndexGreaterOrEqual_commit_unexpectedGreater() {
        LoggingCallback[] callbacks = {newLoggingCallback(), newLoggingCallback()};
        subj.addAll(List.of(callbacks));
        int index = uid();
        doReturn(index + 1).when(callbacks[1]).index();
        doReturn(index).when(callbacks[0]).index();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(index).when(joinPoint).index();
        var context = mock(LoggingContext.class);

        LoggingCallback actual = subj.removeIfIndexGreaterOrEqual(joinPoint, context);

        assertSoftly(s -> {
            s.assertThat(actual).as("actual").isEqualTo(callbacks[0]);
            s.assertThat(subj).as("subj").isEmpty();
        });
        var inOrder = inOrder(joinPoint, callbacks[0], callbacks[1]);
        inOrder.verify(joinPoint).index();
        inOrder.verify(callbacks[1]).index();
        inOrder.verify(callbacks[1]).endEvent();
        inOrder.verify(callbacks[1]).stop(context);
        inOrder.verify(callbacks[0]).resume();
        inOrder.verify(callbacks[1]).joinPoint();
        inOrder.verify(callbacks[1]).logSuccess(null);
        inOrder.verify(callbacks[0]).index();
        inOrder.verify(callbacks[0]).endEvent();
        inOrder.verify(callbacks[0]).stop(context);
        inOrder.verify(callbacks[0]).commit(context);
        verifyNoMoreInteractions(joinPoint, callbacks[0], callbacks[1]);
    }

    LoggingCallback newLoggingCallback() {
        return mock(LoggingCallback.class, "callback" + uid());
    }
}