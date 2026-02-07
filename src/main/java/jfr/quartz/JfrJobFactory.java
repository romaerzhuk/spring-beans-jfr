package jfr.quartz;

import com.google.common.base.Throwables;
import jfr.logging.JfrLoggingService;
import jfr.logging.LoggingJoinPoint;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.List;

/**
 * Обёртывает задачи Quartz Java Flight Recorder-ом.
 *
 * @author Roman_Erzhukov
 */
@RequiredArgsConstructor
public class JfrJobFactory implements JobFactory {
    private final ListableBeanFactory beanFactory;
    private final JfrLoggingService loggingService;

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        JobFactory delegate = beanFactory.getBean(SpringBeanJobFactory.class);
        Job job = delegate.newJob(bundle, scheduler);
        return context -> {
            String execute = "execute";
            var joinPoint = LoggingJoinPoint.of(null, job.getClass(), execute, execute, List.of(context));
            try {
                loggingService.proceedCallback(joinPoint, () -> {
                    job.execute(context);
                    return null;
                });
            } catch (Throwable t) {
                Throwables.throwIfUnchecked(t);
                throw new RuntimeException(t.getMessage(), t);
            }
        };
    }
}
