package jfr.config;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class MultiplyService {
    @Timed("multiply")
    public BigDecimal multiply(BigDecimal value, int n) throws InterruptedException {
        Thread.sleep(10);
        return value.multiply(BigDecimal.valueOf(n));
    }
}
