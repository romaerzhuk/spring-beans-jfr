package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrNonReentrantLoggingContextStrategy}.
 *
 * @author Roman_Erzhukov
 */
@SuppressWarnings("unchecked")
class JfrNonReentrantLoggingContextStrategyTest {
    JfrNonReentrantLoggingContextStrategy subj = new JfrNonReentrantLoggingContextStrategy();

    @Test
    void createContextIfReentrant() {
        var joinPoint = mock(JfrJoinPoint.class);
        Function<JfrJoinPoint, LoggingContext> factory = mock(Function.class);

        LoggingContext actual = subj.createContextIfReentrant(joinPoint, factory);

        assertThat(actual).isNull();
        verifyNoInteractions(joinPoint, factory);
    }

    @Test
    void createUnstartedStopwatchOrNull() {
        var context = mock(LoggingContext.class);

        Stopwatch actual = subj.createUnstartedStopwatchOrNull(context);

        assertThat(actual).isNull();
        verifyNoMoreInteractions(context);
    }

    @Test
    void init() {
        var context = mock(LoggingContext.class);
        var callback = mock(LoggingCallback.class);
        var event = mock(AbstractMethodEvent.class);

        LoggingContext actual = subj.init(context, callback, event);

        assertThat(actual).isEqualTo(context);
        verify(context).beforeNonReentrant(callback, event);
        verifyNoMoreInteractions(context, callback, event);
    }
}