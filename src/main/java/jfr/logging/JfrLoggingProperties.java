package jfr.logging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Настройки.
 *
 * @param threshold       пороговая длительность для записи в JFR.
 * @param logErrorEnabled позволяет включать дополнительную запись stacktrace-ов ошибок. Обычно нет необходимости включать.
 *                        За логирование исключения отвечает перехвативший её код, получится двойная запись в лог.
 *                        Может помочь, если код подавляет исключения.
 * @author Roman_Erzhukov
 */
@Component
record JfrLoggingProperties(@Value("${jfr.threshold:10ms}") Duration threshold,
                            @Value("${jfr.logErrorEnabled:false}") boolean logErrorEnabled) {
}
