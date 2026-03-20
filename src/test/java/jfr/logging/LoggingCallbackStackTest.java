package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static jfr.test.junit.UidExtension.uid;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link LoggingCallbackStack}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
@SuppressWarnings("ResultOfMethodCallIgnored")
class LoggingCallbackStackTest {
    LoggingCallbackStack subj = new LoggingCallbackStack();

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