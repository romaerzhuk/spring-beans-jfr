package jfr.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrReentrantLoggingContextStrategy}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class JfrReentrantLoggingContextStrategyTest {
    @InjectMocks
    JfrReentrantLoggingContextStrategy subj;
    @Mock
    Ticker ticker;

    @Test
    void createContextIfReentrant() {
        var joinPoint = mock(JfrJoinPoint.class);
        Function<JfrJoinPoint, LoggingContext> factory = mock(Function.class);
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(factory).apply(joinPoint);

        LoggingContext actual = subj.createContextIfReentrant(joinPoint, factory);

        assertThat(actual).isEqualTo(expected);
        verifyNoMoreInteractions(ticker, joinPoint, factory, expected);
    }

    @Test
    void createUnstartedStopwatchOrNull() {
        var context = mock(LoggingContext.class);

        Stopwatch actual = subj.createUnstartedStopwatchOrNull(context);

        assertThat(actual.isRunning()).isFalse();
        verifyNoMoreInteractions(ticker, context);
    }

    @Test
    void init() {
        var context = mock(LoggingContext.class);
        var callback = mock(LoggingCallback.class);
        var event = mock(AbstractMethodEvent.class);

        LoggingContext actual = subj.init(context, callback, event);

        assertThat(actual).isEqualTo(context);
        verify(context).before(callback);
        verifyNoMoreInteractions(ticker, context, callback, event);
    }
}