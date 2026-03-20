package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.NonReentrantMethodEvent;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static jfr.test.junit.UidExtension.uid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link NoReentrantCallbackByEventClass}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class NoReentrantCallbackByEventClassTest {
    static class TestEvent1 extends NonReentrantMethodEvent {
    }

    static class TestEvent2 extends NonReentrantMethodEvent {
    }

    static class TestEvent3 extends NonReentrantMethodEvent {
    }

    NoReentrantCallbackByEventClass subj = new NoReentrantCallbackByEventClass();

    @Test
    void removeIfIndexGreaterOrEqual() {
        LoggingCallback[] callback = {newLoggingCallback(), newLoggingCallback(), newLoggingCallback()};
        subj.putAll(Map.of(TestEvent1.class, callback[0], TestEvent2.class, callback[1], TestEvent3.class, callback[2]));
        var joinPoint = mock(JfrJoinPoint.class);
        int index = uid();
        doReturn(index).when(joinPoint).index();
        doReturn(index - 2).when(callback[0]).index();
        doReturn(index - 1).when(callback[1]).index();
        doReturn(index).when(callback[2]).index();

        subj.removeIfIndexGreaterOrEqual(joinPoint);

        assertThat(subj.entrySet()).containsExactlyInAnyOrder(Map.entry(TestEvent1.class, callback[0]), Map.entry(TestEvent2.class, callback[1]));
        var inOrder = inOrder(callback[2]);
        inOrder.verify(callback[2]).endEvent();
        inOrder.verify(callback[2]).logSuccess(null);
        verifyNoMoreInteractions(joinPoint, callback[0], callback[1], callback[2]);
    }

    @Test
    void removeIfIndexGreaterOrEqual_2() {
        LoggingCallback[] callback = {newLoggingCallback(), newLoggingCallback(), newLoggingCallback()};
        subj.putAll(Map.of(TestEvent1.class, callback[0], TestEvent2.class, callback[1], TestEvent3.class, callback[2]));
        var joinPoint = mock(JfrJoinPoint.class);
        int index = uid();
        doReturn(index).when(joinPoint).index();
        doReturn(index - 1).when(callback[0]).index();
        doReturn(index).when(callback[1]).index();
        doReturn(index + 1).when(callback[2]).index();

        subj.removeIfIndexGreaterOrEqual(joinPoint);

        assertThat(subj.entrySet()).containsExactly(Map.entry(TestEvent1.class, callback[0]));
        verify(callback[1]).endEvent();
        verify(callback[1]).logSuccess(null);
        verify(callback[2]).endEvent();
        verify(callback[2]).logSuccess(null);
        verifyNoMoreInteractions(joinPoint, callback[0], callback[1], callback[2]);
    }

    LoggingCallback newLoggingCallback() {
        return mock(LoggingCallback.class, "callback" + uid());
    }
}