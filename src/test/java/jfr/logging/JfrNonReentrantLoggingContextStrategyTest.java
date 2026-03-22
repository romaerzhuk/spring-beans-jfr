package jfr.logging;

import jfr.api.JfrJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
}