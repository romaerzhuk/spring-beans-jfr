package jfr.event;

import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Тесты для {@link AbstractMethodEvent}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith(UidExtension.class)
public class AbstractMethodEventTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testToString(boolean hasClass) {
        var subj = spy(AbstractMethodEvent.class);
        subj.beanClass = hasClass ? getClass() : null;
        String method = subj.method = uidS();

        String actual = subj.toString();

        assertThat(actual).isEqualTo(subj.getClass().getSimpleName() +
                "{beanClass=" + (hasClass ? getClass().getSimpleName() : "null") +
                ", method=" + method + "}");
    }
}