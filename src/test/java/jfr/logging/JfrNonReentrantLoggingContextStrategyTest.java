package jfr.logging;

import jfr.api.LoggingJoinPoint;
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
    void createIfReentrant() {
        var joinPoint = mock(LoggingJoinPoint.class);
        Function<LoggingJoinPoint, LoggingContext> factory = mock(Function.class);

        LoggingContext actual = subj.createIfReentrant(joinPoint, factory);

        assertThat(actual).isNull();
        verifyNoInteractions(joinPoint, factory);
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