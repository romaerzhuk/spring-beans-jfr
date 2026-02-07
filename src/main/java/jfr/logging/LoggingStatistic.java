package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.event.MethodInvocationEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Статистика выполнения вложенных бизнес-методов.
 *
 * @author Roman_Erzhukov
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
class LoggingStatistic {
    /**
     * Количество выполненных вызовов.
     */
    private int count;

    /**
     * Минимальная длительность выполнения, нс.
     */
    private long min = Long.MAX_VALUE;

    /**
     * Максимальная длительность выполнения, нс.
     */
    private long max = Long.MIN_VALUE;

    /**
     * Общая длительность выполнения, нс.
     */
    private long sum;

    /**
     * Событие с максимальным временем выполнения.
     */
    @Nullable
    private MethodInvocationEvent event;

    /**
     * Обновляет статистику.
     *
     * @param stopWatch время выполнения
     * @param event     событие
     */
    public void update(Stopwatch stopWatch, MethodInvocationEvent event) {
        count++;
        long time = stopWatch.elapsed(TimeUnit.NANOSECONDS);
        min = Math.min(time, min);
        if (time > max) {
            max = time;
            this.event = event;
        }
        sum += time;
    }

    /**
     * Пишет статистику в журнал Java Flight Recorder.
     */
    public void commit() {
        if (event == null) {
            return;
        }
        event.count = count;
        event.min = min;
        event.max = max;
        event.sum = sum;
        event.avg = sum / count;
        event.commit();
    }

    public void appendTo(StringBuilder sb, Class<?> clazz, Object method) {
        sb.append("\n\t")
                .append("class=").append(clazz.getSimpleName())
                .append(", method=").append(method)
                .append(", count=").append(count)
                .append(", sum=").append(formatTime(sum))
                .append(", min=").append(formatTime(min))
                .append(", avg=").append(formatTime(sum / count))
                .append(", max=").append(formatTime(max));
    }

    private static String formatTime(long time) {
        return DurationFormatUtils.formatDurationHMS(time / 1000_000);
    }
}
