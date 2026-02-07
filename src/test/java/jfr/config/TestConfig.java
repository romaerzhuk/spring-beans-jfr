package jfr.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAspectJAutoProxy
@Import(JfrConfiguration.class)
@ComponentScan(basePackageClasses = TestConfig.class)
public class TestConfig {
}