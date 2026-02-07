package jfr.config;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Пример сервиса, вызывающего тяжёлый метод другого компонента.
 *
 * @author Roman_Erzhukov
 */
@Component
@RequiredArgsConstructor
public class HelloWorldService {
    private final FactorialService factorialService;

    @Timed("hello")
    public String hello(String name, int n) throws InterruptedException {
        Thread.sleep(30);
        return ("Привет, %s!\n%d! %s").formatted(name, n, factorialService.factorial(n));
    }
}
