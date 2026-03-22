package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import org.junit.jupiter.api.Test;

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
@SuppressWarnings("unchecked")
class JfrReentrantLoggingContextStrategyTest {
    JfrReentrantLoggingContextStrategy subj = new JfrReentrantLoggingContextStrategy();

    @Test
    void createContextIfReentrant() {
        var joinPoint = mock(JfrJoinPoint.class);
        Function<JfrJoinPoint, LoggingContext> factory = mock(Function.class);
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(factory).apply(joinPoint);

        LoggingContext actual = subj.createContextIfReentrant(joinPoint, factory);

        assertThat(actual).isEqualTo(expected);
        verify(factory).apply(joinPoint);
        verifyNoMoreInteractions(joinPoint, factory, expected);
    }

    @Test
    void init() {
        var context = mock(LoggingContext.class);
        var callback = mock(LoggingCallback.class);
        var event = mock(AbstractMethodEvent.class);

        LoggingContext actual = subj.init(context, callback, event);

        assertThat(actual).isEqualTo(context);
        verify(context).before(callback);
        verifyNoMoreInteractions(context, callback, event);
    }
}