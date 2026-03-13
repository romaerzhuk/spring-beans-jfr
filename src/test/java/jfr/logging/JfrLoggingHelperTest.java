package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrLoggingHelper}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class JfrLoggingHelperTest {
    @InjectMocks
    JfrLoggingHelper subj;
    @Mock
    JfrLoggingContextHolder contextHolder;

    @Test
    void before_enabled() {
        var event = mock(AbstractMethodEvent.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        var logger = mock(Logger.class);
        doReturn(true).when(event).isEnabled(joinPoint, logger);
        var strategy = mock(JfrLoggingContextStrategy.class);
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(contextHolder).getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        LoggingContext actual = subj.before(joinPoint, strategy, event, logger);

        assertThat(actual).isEqualTo(expected);
        verifyNoMoreInteractions(contextHolder, event, joinPoint, logger, strategy, expected);
    }

    @Test
    void before_disabled() {
        var event = mock(AbstractMethodEvent.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        var logger = mock(Logger.class);
        doReturn(false).when(event).isEnabled(joinPoint, logger);
        var strategy = mock(JfrLoggingContextStrategy.class);

        LoggingContext actual = subj.before(joinPoint, strategy, event, logger);

        assertThat(actual).isNull();
        verifyNoMoreInteractions(contextHolder, event, joinPoint, logger, strategy);
    }

    @Test
    void afterReturning_notRemoveContext() {
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        Object retVal = uidS();
        doReturn(false).when(context).afterReturning(joinPoint, retVal);

        subj.afterReturning(context, joinPoint, retVal);

        verifyNoMoreInteractions(contextHolder, context, joinPoint);
    }

    @Test
    void afterReturning_removeContext() {
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        Object retVal = uidS();
        doReturn(true).when(context).afterReturning(joinPoint, retVal);

        subj.afterReturning(context, joinPoint, retVal);

        verify(contextHolder).removeContext();
        verifyNoMoreInteractions(contextHolder, context, joinPoint);
    }

    @Test
    void afterThrowing_notRemoveContext() {
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        var thrown = new Throwable(uidS());
        doReturn(false).when(context).afterThrowing(joinPoint, thrown);

        subj.afterThrowing(context, joinPoint, thrown);

        verifyNoMoreInteractions(contextHolder, context, joinPoint);
    }

    @Test
    void afterThrowing_removeContext() {
        var context = mock(LoggingContext.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        var thrown = new Throwable(uidS());
        doReturn(true).when(context).afterThrowing(joinPoint, thrown);

        subj.afterThrowing(context, joinPoint, thrown);

        verify(contextHolder).removeContext();
        verifyNoMoreInteractions(contextHolder, context, joinPoint);
    }
}