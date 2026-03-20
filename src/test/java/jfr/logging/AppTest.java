package jfr.logging;

import jfr.api.JfrLoggingService;
import jfr.logging.test.HelloWorldService;
import jfr.logging.test.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
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
    JfrLoggingService loggingService;
    @MockitoSpyBean
    LoggingCallbackFactory callbackFactory;

    @Test
    void hello() throws Throwable {
        log.debug("test - start");
        var callbacks = new ArrayList<LoggingCallback>();
        doAnswer(inv -> {
            var spy = spy((LoggingCallback) inv.callRealMethod());
            callbacks.add(spy);
            return spy;
        }).when(callbackFactory).create(any(), any(), any(), any(), any());

        String actual = helloWorldService.hello("мир", 70);

        assertThat(actual).isEqualTo("Привет, мир!\n" +
                "70! 11978571669969891796072783721689098736458938142546425857555362864628009582789845319680000000000000000");
        verify(loggingService, times(71)).proceed(any());
        callbacks.forEach(callback ->
                verify(callback, times(callback.index() == 0 ? 1 : 0)).commit(any()));
    }
}
