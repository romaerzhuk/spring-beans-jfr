package jfr.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import jfr.api.JfrJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.lazyCondition;
import static jfr.test.assertj.ConditionsHelper.match;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link  LoggingCallbackFactory}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class LoggingCallbackFactoryTest implements MethodSourceHelper {
    static class TestMethodEvent extends AbstractMethodEvent {
        @Override
        public boolean isReentrant() {
            throw new UnsupportedOperationException();
        }
    }

    @InjectMocks
    LoggingCallbackFactory subj;
    @Mock
    Function<Class<?>, Logger> loggerFactory;
    @Mock
    Ticker ticker;
    @Mock
    JfrLoggingProperties properties;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void create_debugEnabled(boolean logErrorEnabled) {
        class TestClass {
        }
        var logger = mock(Logger.class, "logger");
        doReturn(true).when(logger).isDebugEnabled();
        var targetLogger = mock(Logger.class, "targetLogger");
        doReturn(targetLogger).when(loggerFactory).apply(TestClass.class);
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(TestClass.class).when(joinPoint).targetClass();
        var event = mock(TestMethodEvent.class);
        doReturn(logErrorEnabled).when(properties).logErrorEnabled();
        String name = uidS();
        doReturn(name).when(joinPoint).name();
        Object method = uidS();
        doReturn(method).when(joinPoint).method();
        var context = mock(LoggingContext.class);
        var callbacks = mock(LoggingCallbackStack.class);
        doReturn(callbacks).when(context).callbacks();
        int size = uid();
        doReturn(size).when(callbacks).size();
        var args = List.of(uidS(), uid());
        doReturn(args).when(joinPoint).args();

        LoggingCallback actual = subj.create(joinPoint, event, logger, context);

        assertThat(actual).is(loggingCallback(joinPoint, event, targetLogger, logErrorEnabled, name, method, size, args));
        verifyNoMoreInteractions(loggerFactory, ticker, properties, logger, targetLogger, joinPoint, event, context, callbacks);

        actual.stopwatch().start();
        verify(ticker).read();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void create_debugDisabled(boolean logErrorEnabled) {
        class TestClass {
        }
        var logger = mock(Logger.class, "logger");
        doReturn(false).when(logger).isDebugEnabled();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(TestClass.class).when(joinPoint).targetClass();
        var event = mock(TestMethodEvent.class);
        doReturn(logErrorEnabled).when(properties).logErrorEnabled();
        String name = uidS();
        doReturn(name).when(joinPoint).name();
        Object method = uidS();
        doReturn(method).when(joinPoint).method();
        var context = mock(LoggingContext.class);
        var callbacks = mock(LoggingCallbackStack.class);
        doReturn(callbacks).when(context).callbacks();
        int size = uid();
        doReturn(size).when(callbacks).size();

        LoggingCallback actual = subj.create(joinPoint, event, logger, context);

        assertThat(actual).is(loggingCallback(joinPoint, event, null, logErrorEnabled, name, method, size, null));
        verifyNoMoreInteractions(loggerFactory, ticker, properties, logger, joinPoint, event, context, callbacks);

        actual.stopwatch().start();
        verify(ticker).read();
    }

    Condition<LoggingCallback> loggingCallback(JfrJoinPoint joinPoint,
                                               AbstractMethodEvent event,
                                               Logger logger,
                                               boolean logErrorEnabled,
                                               String name,
                                               Object method,
                                               int index,
                                               List<?> args) {
        return lazyCondition(actual -> nestable("LoggingCallback",
                isEqual("joinPoint", actual.joinPoint(), joinPoint),
                isEqual("event", actual.event(), event),
                isEqual("logger", actual.logger(), logger),
                isEqual("logErrorEnabled", actual.logErrorEnabled(), logErrorEnabled),
                isEqual("name", actual.name(), name),
                isEqual("method", actual.method(), method),
                isEqual("index", actual.index(), index),
                match("stopwatch", actual.stopwatch(), instanceOf(Stopwatch.class)),
                isEqual("stopwatch.running", actual.stopwatch().isRunning(), false),
                isEqual("args", actual.args(), args)
        ));
    }
}