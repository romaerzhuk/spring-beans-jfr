package jfr.quartz;

import jfr.api.JfrJoinPoint;
import jfr.api.JfrJoinPointFactory;
import jfr.api.JfrLoggingService;
import jfr.api.JoinPointCallback;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.List;
import java.util.stream.Stream;

import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.match;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrJobFactory}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class JfrJobFactoryTest {
    @InjectMocks
    JfrJobFactory subj;
    @Mock
    ListableBeanFactory beanFactory;
    @Mock
    JfrJoinPointFactory joinPointFactory;
    @Mock
    JfrLoggingService loggingService;

    @Test
    void newJob() throws Throwable {
        var delegate = mock(SpringBeanJobFactory.class);
        doReturn(delegate).when(beanFactory).getBean(SpringBeanJobFactory.class);
        var bundle = mock(TriggerFiredBundle.class);
        var scheduler = mock(Scheduler.class);
        var job = mock(Job.class);
        doReturn(job).when(delegate).newJob(bundle, scheduler);
        var context = mock(JobExecutionContext.class);
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(joinPoint).when(joinPointFactory).create(job.getClass(), "execute", "execute", List.of(context));
        doAnswer(inv -> {
            JoinPointCallback callback = inv.getArgument(1);
            verify(job, never()).execute(any());

            assertThat(callback.proceed()).isNull();
            verify(job).execute(context);
            return null;
        }).when(loggingService).proceedCallback(eq(joinPoint), any());

        Job actual = subj.newJob(bundle, scheduler);

        verify(beanFactory).getBean(SpringBeanJobFactory.class);
        verify(delegate).newJob(any(), any());
        verifyNoMoreInteractions(beanFactory, joinPointFactory, loggingService, delegate, bundle, scheduler, job, context, joinPoint);

        actual.execute(context);

        var inOrder = inOrder(joinPointFactory, loggingService, job);
        inOrder.verify(joinPointFactory).create(any(), any(), any(), any());
        inOrder.verify(loggingService).proceedCallback(any(), any());
        inOrder.verify(job).execute(any());
        verifyNoMoreInteractions(beanFactory, joinPointFactory, loggingService, delegate, bundle, scheduler, job, context, joinPoint);
    }

    @MethodSource
    @ParameterizedTest
    void newJob_exceptions(Throwable thrown) throws Throwable {
        var delegate = mock(SpringBeanJobFactory.class);
        doReturn(delegate).when(beanFactory).getBean(SpringBeanJobFactory.class);
        var bundle = mock(TriggerFiredBundle.class);
        var scheduler = mock(Scheduler.class);
        var job = mock(Job.class);
        doReturn(job).when(delegate).newJob(bundle, scheduler);
        var context = mock(JobExecutionContext.class);
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(joinPoint).when(joinPointFactory).create(job.getClass(), "execute", "execute", List.of(context));
        doAnswer(inv -> {
            JoinPointCallback callback = inv.getArgument(1);
            verify(job, never()).execute(any());

            assertThat(callback.proceed()).isNull();
            verify(job).execute(context);
            if (thrown != null) {
                throw thrown;
            }
            return null;
        }).when(loggingService).proceedCallback(eq(joinPoint), any());
        boolean unchecked = thrown instanceof RuntimeException || thrown instanceof Error;

        Job actual = subj.newJob(bundle, scheduler);

        verify(beanFactory).getBean(SpringBeanJobFactory.class);
        verify(delegate).newJob(any(), any());
        verifyNoMoreInteractions(beanFactory, joinPointFactory, loggingService, delegate, bundle, scheduler, job, context, joinPoint);

        var t = assertThrows(Throwable.class, () -> actual.execute(context));

        assertThat(t).is(nestable("Throwable",
                match("this", t, unchecked ? sameInstance(t) : not(sameInstance(thrown))),
                isEqual("class", t.getClass(), unchecked ? thrown.getClass() : RuntimeException.class),
                isEqual("message", t.getMessage(), thrown.getMessage()),
                isEqual("cause", t.getCause(), unchecked ? thrown.getCause() : thrown)
        ));
        var inOrder = inOrder(joinPointFactory, loggingService, job);
        inOrder.verify(joinPointFactory).create(any(), any(), any(), any());
        inOrder.verify(loggingService).proceedCallback(any(), any());
        inOrder.verify(job).execute(any());
        verifyNoMoreInteractions(beanFactory, joinPointFactory, loggingService, delegate, bundle, scheduler, job, context, joinPoint);
    }

    static Stream<Throwable> newJob_exceptions() {
        return Stream.of(new RuntimeException(uidS()), new Exception(uidS()), new Error(uidS()), new Throwable(uidS()));
    }
}