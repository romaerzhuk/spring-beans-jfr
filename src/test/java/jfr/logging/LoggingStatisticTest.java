package jfr.logging;

import com.google.common.base.Stopwatch;
import jfr.event.MethodInvocationEvent;
import jfr.test.junit.MethodSourceHelper;
import jfr.test.junit.UidExtension;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static jfr.test.hamcrest.PropertiesMatcher.matching;
import static jfr.test.junit.UidExtension.uid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link LoggingStatistic}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith(UidExtension.class)
public class LoggingStatisticTest {
    @MethodSource
    @ParameterizedTest
    void update(int minOffset, int maxOffset) {
        int count = uid();
        long sum = uid();
        long time = uid();
        long min = time + minOffset;
        long max = time + maxOffset;
        var subj = new LoggingStatistic()
                .setCount(count)
                .setSum(sum)
                .setMin(min)
                .setMax(max);
        var stopWatch = mock(Stopwatch.class);
        doReturn(time).when(stopWatch).elapsed(TimeUnit.NANOSECONDS);
        var event = mock(MethodInvocationEvent.class);

        subj.update(stopWatch, event);

        assertThat(subj).is(matching(matcher -> matcher
                .add("count", subj.getCount(), count + 1)
                .add("sum", subj.getSum(), sum + time)
                .add("min", subj.getMin(), minOffset < 0 ? min : time)
                .add("max", subj.getMax(), maxOffset > 0 ? max : time)
        ));
    }

    static Stream<Arguments> update() {
        return MethodSourceHelper.join(
                Stream.of(-5, -2, -1, 0, 1, 2, 5),
                Stream.of(-5, -2, -1, 0, 1, 2, 5));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void commit(boolean hasEvent) {
        int count = uid();
        long min = uid();
        long avg = uid();
        long max = uid();
        long sum = avg * count;
        var event = mock(MethodInvocationEvent.class);
        var subj = new LoggingStatistic()
                .setCount(count)
                .setSum(sum)
                .setMin(min)
                .setMax(max)
                .setEvent(hasEvent ? event : null);
        lenient().doAnswer(inv -> {
            assertThat(event).is(matching(matcher -> matcher
                    .add("count", event.count, count)
                    .add("min", event.min, min)
                    .add("avg", event.avg, avg)
                    .add("max", event.max, max)
                    .add("sum", event.sum, sum)
            ));
            return null;
        }).when(event).commit();

        subj.commit();

        verify(event, times(hasEvent ? 1 : 0)).commit();
        verifyNoMoreInteractions(event);
    }

    @Test
    void appendTo() {
        class TestClass {
        }
        long n = 1000_000;
        int count = uid();
        long avg = uid() * n;
        long sum = count * avg;
        long min = uid() * n;
        long max = uid() * n;
        var subj = new LoggingStatistic()
                .setCount(count)
                .setSum(sum)
                .setMin(min)
                .setMax(max);
        var sb = new StringBuilder();
        var method = uid();

        subj.appendTo(sb, TestClass.class, method);

        assertThat(sb.toString())
                .isEqualTo(String.format("\n\tclass=%s, method=%s, count=%s, sum=%s, min=%s, avg=%s, max=%s",
                        TestClass.class.getSimpleName(), method, count, str(sum, n), str(min, n), str(avg, n), str(max, n)));
    }

    String str(long time, long n) {
        return DurationFormatUtils.formatDurationHMS(time / n);
    }
}