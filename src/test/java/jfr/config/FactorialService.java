package jfr.config;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Пример тяжёлого компонена, показывает вызов в цикле.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FactorialService {
    private final MultiplyService multiplyService;

    @Timed("factorial")
    public BigDecimal factorial(int n) throws InterruptedException {
        log.debug("factorial - start");
        var value = BigDecimal.ONE;
        for (int i = 2; i <= n; i++) {
            value = multiplyService.multiply(value, i);
        }
        Thread.sleep(50);
        log.debug("factorial - end");
        return value;
    }
}
