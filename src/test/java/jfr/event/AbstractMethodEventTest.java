package jfr.event;

import jfr.api.JfrJoinPoint;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link AbstractMethodEvent}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class AbstractMethodEventTest implements MethodSourceHelper {
    static class TestMethodEvent extends AbstractMethodEvent {
    }

    @Spy
    TestMethodEvent subj;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testToString(boolean hasClass) {
        subj.beanClass = hasClass ? getClass() : null;
        String method = subj.method = uidS();

        String actual = subj.toString();

        assertThat(actual).isEqualTo(subj.getClass().getSimpleName() +
                "{beanClass=" + (hasClass ? getClass().getSimpleName() : "null") +
                ", method=" + method + "}");
    }

    @ParameterizedTest
    @MethodSource("booleans2")
    void isEnabled(boolean enabled, boolean debug) {
        doReturn(enabled).when(subj).isEnabled();
        var joinPoint = mock(JfrJoinPoint.class);
        var logger = mock(Logger.class);
        doReturn(debug).when(logger).isDebugEnabled();

        boolean actual = subj.isEnabled(joinPoint, logger);

        assertThat(actual).isEqualTo(enabled || debug);
    }
}