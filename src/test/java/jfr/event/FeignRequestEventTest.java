package jfr.event;

import feign.Client;
import jfr.logging.LoggingJoinPoint;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link FeignRequestEvent}.
 *
 * @author Roman_Erzhukov
 */
public class FeignRequestEventTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void test(boolean expected) {
        var subj = new FeignRequestEvent();
        var joinPoint = mock(LoggingJoinPoint.class);
        var client = mock(Client.class);
        doReturn(expected ? client.getClass() : getClass()).when(joinPoint).targetClass();

        boolean actual = subj.test(joinPoint);

        assertThat(actual).isEqualTo(expected);
    }
}