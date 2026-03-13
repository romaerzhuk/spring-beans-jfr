package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrLoggingContextHolder}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class JfrLoggingContextHolderTest {
    @Spy
    @InjectMocks
    JfrLoggingContextHolder subj;
    @Autowired
    @Mock
    ThreadLocal<LoggingContext> context;
    @Autowired
    @Mock
    Function<LoggingJoinPoint, LoggingContext> contextFactory;
    @Autowired
    @Mock
    LoggingCallbackFactory callbackFactory;

    @Test
    void getOrCreateIfReentrant_create() {
        doReturn(null).when(subj).getContext();
        var joinPoint = mock(LoggingJoinPoint.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        var created = mock(LoggingContext.class, "created");
        doReturn(created).when(strategy).createIfReentrant(joinPoint, contextFactory);
        doNothing().when(subj).setContext(any());
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbackFactory).create(joinPoint, event, logger);
        var expected = mock(LoggingContext.class, "expected");
        doReturn(expected).when(strategy).init(created, callback, event);

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isEqualTo(expected);
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verify(subj).setContext(created);
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory,
                joinPoint, strategy, created, event, logger, callback, expected);
    }

    @Test
    void getOrCreateIfReentrant_exists() {
        var joinPoint = mock(LoggingJoinPoint.class);
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        var found = mock(LoggingContext.class, "found");
        doReturn(found).when(subj).getContext();
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbackFactory).create(joinPoint, event, logger);
        var expected = mock(LoggingContext.class, "expected");
        doReturn(expected).when(strategy).init(found, callback, event);

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isEqualTo(expected);
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory,
                joinPoint, event, logger, strategy, found, callback, expected);

    }

    @Test
    void getOrCreateIfReentrant_notExistsAndNotReentrant() {
        var joinPoint = mock(LoggingJoinPoint.class);
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        doReturn(null).when(subj).getContext();

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isNull();
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verify(strategy).createIfReentrant(joinPoint, contextFactory);
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory,
                joinPoint, event, logger, strategy);
    }

    @Test
    void getContext() {
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(context).get();

        LoggingContext actual = subj.getContext();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setContext() {
        var ctx = mock(LoggingContext.class);

        subj.setContext(ctx);

        verify(context).set(ctx);
    }

    @Test
    void destroy() {
        subj.destroy();

        verify(context).remove();
    }

    @Test
    void removeContext() {
        subj.removeContext();

        verify(context).remove();
    }
}