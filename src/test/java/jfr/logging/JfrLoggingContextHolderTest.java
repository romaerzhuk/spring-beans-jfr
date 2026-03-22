package jfr.logging;

import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.UidExtension;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Function;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.lazyCondition;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
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
    Function<JfrJoinPoint, LoggingContext> contextFactory;
    @Autowired
    @Mock
    LoggingCallbackFactory callbackFactory;

    @Test
    void create() {
        var found = mock(LoggingContext.class);
        doReturn(found).when(subj).getContext();
        var callbacks = mock(LoggingCallbackStack.class);
        doReturn(callbacks).when(found).callbacks();
        int size = uid();
        doReturn(size).when(callbacks).size();
        var targetClass = getClass();
        Object name = uidS();
        Object method = uidS();
        List<Object> args = List.of(uid(), uidS());

        JfrJoinPoint actual = subj.create(targetClass, name, method, args);

        assertThat(actual).is(loggingJoinPoint(size, targetClass, name, method, args));
        verify(subj).create(any(), any(), any(), any());
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, found, callbacks);
    }

    @Test
    void create_contextIsNull() {
        doReturn(null).when(subj).getContext();
        var targetClass = getClass();
        Object name = uidS();
        Object method = uidS();
        List<Object> args = List.of(uid(), uidS());

        JfrJoinPoint actual = subj.create(targetClass, name, method, args);

        assertThat(actual).is(loggingJoinPoint(0, targetClass, name, method, args));
        verify(subj).create(any(), any(), any(), any());
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory);
    }

    @Test
    void wrap() throws Exception {
        var found = mock(LoggingContext.class);
        doReturn(found).when(subj).getContext();
        var callbacks = mock(LoggingCallbackStack.class);
        doReturn(callbacks).when(found).callbacks();
        int size = uid();
        doReturn(size).when(callbacks).size();
        var joinPoint = mock(JoinPoint.class);
        doReturn(this).when(joinPoint).getTarget();
        var signature = mock(MethodSignature.class);
        doReturn(signature).when(joinPoint).getSignature();
        String name = uidS();
        doReturn(name).when(signature).getName();
        var method = getClass().getDeclaredMethod("wrap");
        doReturn(method).when(signature).getMethod();
        List<Object> args = List.of(uid(), uidS());
        doReturn(args.toArray()).when(joinPoint).getArgs();

        JfrJoinPoint actual = subj.wrap(joinPoint);

        assertThat(actual).is(loggingJoinPoint(size, getClass(), name, method, args));
        verify(subj).wrap(any());
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, found, callbacks, joinPoint, signature);
    }

    @Test
    void wrap_contextIsNull() throws Exception {
        doReturn(null).when(subj).getContext();
        var joinPoint = mock(JoinPoint.class);
        doReturn(this).when(joinPoint).getTarget();
        var signature = mock(MethodSignature.class);
        doReturn(signature).when(joinPoint).getSignature();
        String name = uidS();
        doReturn(name).when(signature).getName();
        var method = getClass().getDeclaredMethod("wrap_contextIsNull");
        doReturn(method).when(signature).getMethod();
        List<Object> args = List.of(uid(), uidS());
        doReturn(args.toArray()).when(joinPoint).getArgs();

        JfrJoinPoint actual = subj.wrap(joinPoint);

        assertThat(actual).is(loggingJoinPoint(0, getClass(), name, method, args));
        verify(subj).wrap(any());
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, joinPoint, signature);
    }

    Condition<JfrJoinPoint> loggingJoinPoint(int index, Class<?> targetClass, Object name, Object method, List<Object> args) {
        return lazyCondition(actual -> nestable("LoggingJoinPoint",
                isEqual("index", actual.index(), index),
                isEqual("targetClass", actual.targetClass(), targetClass),
                isEqual("name", actual.name(), name),
                isEqual("method", actual.method(), method),
                isEqual("args", actual.args(), args)
        ));
    }

    @Test
    void getOrCreateIfReentrant_create() {
        doReturn(null).when(subj).getContext();
        var joinPoint = mock(JfrJoinPoint.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(strategy).createContextIfReentrant(joinPoint, contextFactory);
        doNothing().when(subj).setContext(any());
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbackFactory).create(joinPoint, event, logger, expected);

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isEqualTo(expected);
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verify(subj).setContext(expected);
        verify(expected).before(callback);
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, joinPoint, strategy, expected, event, logger, callback);
    }

    @Test
    void getOrCreateIfReentrant_exists() {
        var joinPoint = mock(JfrJoinPoint.class);
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        var expected = mock(LoggingContext.class);
        doReturn(expected).when(subj).getContext();
        var callback = mock(LoggingCallback.class);
        doReturn(callback).when(callbackFactory).create(joinPoint, event, logger, expected);

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isEqualTo(expected);
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verify(expected).before(callback);
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, joinPoint, event, logger, strategy, expected, callback);
    }

    @Test
    void getOrCreateIfReentrant_notExistsAndNotReentrant() {
        var joinPoint = mock(JfrJoinPoint.class);
        var event = mock(AbstractMethodEvent.class);
        var logger = mock(Logger.class);
        var strategy = mock(JfrLoggingContextStrategy.class);
        doReturn(null).when(subj).getContext();

        LoggingContext actual = subj.getOrCreateIfReentrant(joinPoint, event, logger, strategy);

        assertThat(actual).isNull();
        verify(subj).getOrCreateIfReentrant(any(), any(), any(), any());
        verify(strategy).createContextIfReentrant(joinPoint, contextFactory);
        verifyNoMoreInteractions(subj, context, contextFactory, callbackFactory, joinPoint, event, logger, strategy);
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