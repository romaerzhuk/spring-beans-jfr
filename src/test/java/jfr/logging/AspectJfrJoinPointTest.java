package jfr.logging;

import jfr.test.junit.UidExtension;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;

import java.util.List;

import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link AspectJfrJoinPoint}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class AspectJfrJoinPointTest {
    static class TestTarget {
        public void test() {
        }
    }

    AspectJfrJoinPoint subj;

    @Mock
    JoinPoint joinPoint;

    @BeforeEach
    void init() {
        subj = new AspectJfrJoinPoint(uid(), joinPoint);
    }

    @Test
    void targetClass_simple() {
        var advised = mock(Advised.class);
        doReturn(new TestTarget()).when(joinPoint).getTarget();
        var source = mock(TargetSource.class);

        Class<?> actual = subj.targetClass();

        assertThat(actual).isEqualTo(TestTarget.class);
        verifyNoMoreInteractions(advised, source);
    }

    @Test
    void targetClass_advised() {
        var advised = mock(Advised.class);
        doReturn(advised).when(joinPoint).getTarget();
        var source = mock(TargetSource.class);
        doReturn(source).when(advised).getTargetSource();
        doReturn(TestTarget.class).when(source).getTargetClass();

        Class<?> actual = subj.targetClass();

        assertThat(actual).isEqualTo(TestTarget.class);
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

    @Test
    void method_methodSignature() throws Exception {
        var signature = mock(MethodSignature.class);
        var expected = TestTarget.class.getMethod("test");
        doReturn(expected).when(signature).getMethod();
        doReturn(signature).when(joinPoint).getSignature();

        Object actual = subj.method();

        assertThat(actual).isEqualTo(expected);
        verify(signature).getMethod();
        verifyNoMoreInteractions(signature);
    }

    @Test
    void method_signature() {
        var signature = mock(Signature.class);
        doReturn(signature).when(joinPoint).getSignature();
        String expected = uidS();
        doReturn(expected).when(signature).getName();

        Object actual = subj.method();

        assertThat(actual).isEqualTo(expected);
        verifyNoMoreInteractions(signature);
    }

    @Test
    void args() {
        Object[] args = {uidS(), null, uid()};
        doReturn(args).when(joinPoint).getArgs();

        List<Object> actual = subj.args();

        assertThat(actual).containsExactly(args);
    }
}