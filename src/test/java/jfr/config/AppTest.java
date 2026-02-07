package jfr.config;

import jfr.logging.JfrLoggingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тест запуска Spring boot-приложения и демонстрации работы аспекта.
 *
 * @author Roman_Erzhukov
 */
@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestConfig.class)
public class AppTest {
    @Autowired
    HelloWorldService helloWorldService;
    @MockitoSpyBean
    JfrLoggingServiceImpl<?> loggingService;

    @Test
    void hello() throws Throwable {
        log.debug("test - start");

        String actual = helloWorldService.hello("мир", 70);

        assertThat(actual).isEqualTo("Привет, мир!\n" +
                "70! 11978571669969891796072783721689098736458938142546425857555362864628009582789845319680000000000000000");
        verify(loggingService, times(71)).proceed(any());
    }
}
