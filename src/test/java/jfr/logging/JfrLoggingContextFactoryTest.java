package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static java.time.Duration.ofNanos;
import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.junit.UidExtension.newInstant;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link JfrLoggingContextFactory}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class JfrLoggingContextFactoryTest {
    @InjectMocks
    JfrLoggingContextFactory subj;
    @Mock
    JfrLoggingProperties properties;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void apply(boolean hasIdentity) {
        long threshold = newInstant().toEpochMilli();
        doReturn(ofNanos(threshold)).when(properties).threshold();
        var joinPoint = mock(LoggingJoinPoint.class);
        Object identityPoint = hasIdentity ? uidS() : null;
        doReturn(identityPoint).when(joinPoint).identityPoint();

        LoggingContext actual = subj.apply(joinPoint);

        assertThat(actual).is(nestable("LoggingContext",
                isEqual("identityPoint", actual.identityPoint, hasIdentity ? identityPoint : joinPoint),
                isEqual("logger", actual.logger, LoggerFactory.getLogger(LoggingContext.class)),
                isEqual("thresholdNanos", actual.thresholdNanos, threshold)
        ));
    }
}