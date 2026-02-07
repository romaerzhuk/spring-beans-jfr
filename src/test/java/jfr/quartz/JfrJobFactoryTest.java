package jfr.quartz;

import jfr.logging.JfrLoggingService;
import jfr.logging.JoinPointCallback;
import jfr.logging.LoggingJoinPoint;
import jfr.test.junit.UidExtension;
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

import static java.util.Arrays.stream;
import static jfr.test.assertj.ConditionsHelper.isEqual;
import static jfr.test.assertj.ConditionsHelper.match;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.NestableCondition.nestable;
import static org.assertj.core.util.Arrays.array;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    JfrLoggingService loggingService;

    @MethodSource
    @ParameterizedTest
    void newJob(Throwable thrown) throws Throwable {
        var delegate = mock(SpringBeanJobFactory.class);
        doReturn(delegate).when(beanFactory).getBean(SpringBeanJobFactory.class);
        var bundle = mock(TriggerFiredBundle.class);
        var scheduler = mock(Scheduler.class);
        var job = mock(Job.class);
        doReturn(job).when(delegate).newJob(bundle, scheduler);
        var context = mock(JobExecutionContext.class);
        doAnswer(inv -> {
            LoggingJoinPoint joinPoint = inv.getArgument(0);
            JoinPointCallback callback = inv.getArgument(1);
            verify(job, never()).execute(any());

            assertThat(callback.proceed()).isNull();
            assertThat(joinPoint).isEqualTo(LoggingJoinPoint.of(null, job.getClass(), "execute", "execute", List.of(context)));
            if (thrown != null) {
                throw thrown;
            }
            return null;
        }).when(loggingService).proceedCallback(any(), any());
        boolean unchecked = thrown instanceof RuntimeException || thrown instanceof Error;

        Job actual = subj.newJob(bundle, scheduler);

        verify(delegate).newJob(any(), any());
        verifyNoMoreInteractions(delegate, loggingService, bundle, scheduler, job, context);

        if (thrown == null) {
            actual.execute(context);
        } else {
            var t = assertThrows(Throwable.class, () -> actual.execute(context));
            assertThat(t).is(nestable("Throwable",
                    match("this", t, unchecked ? sameInstance(t) : not(sameInstance(thrown))),
                    isEqual("class", t.getClass(), unchecked ? thrown.getClass() : RuntimeException.class),
                    isEqual("message", t.getMessage(), thrown.getMessage()),
                    isEqual("cause", t.getCause(), unchecked ? thrown.getCause() : thrown)
            ));
        }
        var inOrder = inOrder(job, loggingService);
        inOrder.verify(loggingService).proceedCallback(any(), any());
        inOrder.verify(job).execute(any());
        verifyNoMoreInteractions(beanFactory, loggingService, delegate, bundle, scheduler, job, context);
    }

    static Stream<Throwable> newJob() {
        return stream(array(null, new RuntimeException(uidS()), new Exception(uidS()), new Error(uidS()), new Throwable(uidS())));
    }
}