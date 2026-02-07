package jfr.config;

import com.google.common.base.Ticker;
import jfr.event.NonReentrantMethodEvent;
import jfr.feign.JfrFeignRequestInterceptor;
import jfr.logging.JfrLoggingServiceImpl;
import jfr.quartz.JfrJobFactory;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Компоненты для записи статистики выполнения методов Spring Bean-ов в JFR.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@Configuration
public class JfrConfiguration {
    @Bean
    <E extends NonReentrantMethodEvent> JfrLoggingServiceImpl<E> jfrLoggingService() {
        return new JfrLoggingServiceImpl<>(Ticker.systemTicker(), LoggerFactory::getLogger);
    }

    @Bean
    @ConditionalOnProperty(value = "jfr.quartz.enabled", havingValue = "true")
    JfrJobFactory jfrJobFactory(ListableBeanFactory beanFactory) {
        return new JfrJobFactory(beanFactory, jfrLoggingService());
    }

    @Bean
    @ConditionalOnProperty(value = "jfr.feign.enabled", havingValue = "true")
    JfrFeignRequestInterceptor jfrFeignRequestInterceptor() {
        return new JfrFeignRequestInterceptor(jfrLoggingService());
    }

    @Bean
    @ConditionalOnBean(JfrJobFactory.class)
    public BeanPostProcessor jfrSchedulerFactoryBeanPostProcessor(JfrJobFactory jobFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
                if (bean instanceof SchedulerFactoryBean factoryBean) {
                    log.info("{} setJobFactory {}", beanName, jobFactory);
                    factoryBean.setJobFactory(jobFactory);
                }
                return bean;
            }
        };
    }
}
