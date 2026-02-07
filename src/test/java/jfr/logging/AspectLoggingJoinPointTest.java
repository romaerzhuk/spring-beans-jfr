package jfr.logging;

import jfr.test.junit.UidExtension;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;

import java.util.List;

import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link AspectLoggingJoinPoint}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class AspectLoggingJoinPointTest {
    static class TestTarget {
        public void test() {
        }
    }

    @InjectMocks
    AspectLoggingJoinPoint subj;

    @Mock
    JoinPoint joinPoint;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void targetClass(boolean isAdvised) {
        var advised = mock(Advised.class);
        doReturn(isAdvised ? advised : new TestTarget()).when(joinPoint).getTarget();
        var source = mock(TargetSource.class);
        lenient().doReturn(source).when(advised).getTargetSource();
        lenient().doReturn(TestTarget.class).when(source).getTargetClass();

        Class<?> actual = subj.targetClass();

        assertThat(actual).isEqualTo(TestTarget.class);
        verify(advised, times(isAdvised ? 1 : 0)).getTargetSource();
        verify(source, times(isAdvised ? 1 : 0)).getTargetClass();
        verifyNoMoreInteractions(advised, source);
    }

    @Test
    void name() {
        var signature = mock(Signature.class);
        doReturn(signature).when(joinPoint).getSignature();
        String expected = uidS();
        doReturn(expected).when(signature).getName();

        String actual = subj.name();

        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void method(boolean isMethodSignature) throws Exception {
        var methodSignature = mock(MethodSignature.class);
        var method = TestTarget.class.getMethod("test");
        lenient().doReturn(method).when(methodSignature).getMethod();
        var signature = mock(Signature.class);
        doReturn(isMethodSignature ? methodSignature : signature).when(joinPoint).getSignature();
        String name = uidS();
        lenient().doReturn(name).when(signature).getName();

        Object actual = subj.method();

        assertThat(actual).isSameAs(isMethodSignature ? method : name);
        verify(methodSignature, times(isMethodSignature ? 1 : 0)).getMethod();
        verify(signature, times(isMethodSignature ? 0 : 1)).getName();
        verifyNoMoreInteractions(methodSignature, signature);
    }

    @Test
    void args() {
        Object[] args = {uidS(), null, uid()};
        doReturn(args).when(joinPoint).getArgs();

        List<Object> actual = subj.args();

        assertThat(actual).containsExactly(args);
    }
}