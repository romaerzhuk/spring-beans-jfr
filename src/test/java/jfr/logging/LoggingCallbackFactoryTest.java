package jfr.logging;

import jfr.api.LoggingJoinPoint;
import jfr.event.AbstractMethodEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.function.Function;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.lazyCondition;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link  LoggingCallbackFactory}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
class LoggingCallbackFactoryTest implements MethodSourceHelper {
    static class TestMethodEvent extends AbstractMethodEvent {
    }

    @InjectMocks
    LoggingCallbackFactory subj;
    @Mock
    Function<Class<?>, Logger> loggerFactory;
    @Mock
    JfrLoggingProperties properties;

    @ParameterizedTest
    @MethodSource("booleans2")
    void create_debugEnabled(boolean enabled, boolean logErrorEnabled) {
        class TestClass {
        }
        var logger = mock(Logger.class, "logger");
        doReturn(true).when(logger).isDebugEnabled();
        var targetLogger = mock(Logger.class, "targetLogger");
        doReturn(targetLogger).when(loggerFactory).apply(TestClass.class);
        var joinPoint = mock(LoggingJoinPoint.class);
        doReturn(TestClass.class).when(joinPoint).targetClass();
        var event = mock(TestMethodEvent.class);
        doReturn(enabled).when(event).isEnabled();
        doReturn(logErrorEnabled).when(properties).logErrorEnabled();
        String name = uidS();
        doReturn(name).when(joinPoint).name();
        Object method = uidS();
        doReturn(method).when(joinPoint).method();

        LoggingCallback actual = subj.create(joinPoint, event, logger);

        assertThat(actual).is(loggingCallback(joinPoint, enabled ? event : null, targetLogger, logErrorEnabled, name, method));
        verifyNoMoreInteractions(loggerFactory, properties, logger, targetLogger, joinPoint, event);
    }

    @ParameterizedTest
    @MethodSource("booleans2")
    void create_debugDisabled(boolean enabled, boolean logErrorEnabled) {
        class TestClass {
        }
        var logger = mock(Logger.class, "logger");
        doReturn(false).when(logger).isDebugEnabled();
        var joinPoint = mock(LoggingJoinPoint.class);
        doReturn(TestClass.class).when(joinPoint).targetClass();
        var event = mock(TestMethodEvent.class);
        doReturn(enabled).when(event).isEnabled();
        doReturn(logErrorEnabled).when(properties).logErrorEnabled();
        String name = uidS();
        doReturn(name).when(joinPoint).name();
        Object method = uidS();
        doReturn(method).when(joinPoint).method();

        LoggingCallback actual = subj.create(joinPoint, event, logger);

        assertThat(actual).is(loggingCallback(joinPoint, enabled ? event : null, null, logErrorEnabled, name, method));
        verifyNoMoreInteractions(loggerFactory, properties, logger, joinPoint, event);
    }

    Condition<LoggingCallback> loggingCallback(LoggingJoinPoint joinPoint,
                                               AbstractMethodEvent event,
                                               Logger logger,
                                               boolean logErrorEnabled,
                                               String name,
                                               Object method) {
        return lazyCondition(actual -> nestable("LoggingCallback",
                isEqual("joinPoint", actual.joinPoint, joinPoint),
                isEqual("event", actual.event, event),
                isEqual("logger", actual.logger, logger),
                isEqual("logErrorEnabled", actual.logErrorEnabled, logErrorEnabled),
                isEqual("name", actual.name, name),
                isEqual("method", actual.method, method),
                isEqual("prev", actual.prev, null),
                isEqual("stopwatch", actual.stopwatch, null),
                isEqual("args", actual.args, null)
        ));
    }
}