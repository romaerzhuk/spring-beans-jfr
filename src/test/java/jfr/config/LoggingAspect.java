package jfr.config;

import jfr.logging.JfrLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Пример настройки аспекта для регистрации в лог и журнал Java Flight Recorder статистику времени выполнения бизнес-методов.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    private final JfrLoggingService service;

    @Pointcut("@annotation(io.micrometer.core.annotation.Timed)")
    public void timed() {
    }

//    @Pointcut("@annotation(org.springframework.kafka.annotation.KafkaListener)")
//    public void kafkaListener() {
//    }
//
//    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
//    public void scheduled() {
//    }
//
//    @Pointcut("this(feign.Client)")
//    public void feign() {
//    }

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Around("publicMethod() && timed()")
//            " || kafkaListener()" +
//            " || scheduled()" +
//            " || feign()" +
//            ")")
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return service.proceed(joinPoint);
    }
}
