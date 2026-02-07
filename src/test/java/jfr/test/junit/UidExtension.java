package jfr.test.junit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_HOUR;

/**
 * UidExtension возвращает короткий уникальный идентификатор в рамках запуска тестов.
 *
 * @author Roman_Erzhukov
 */
public class UidExtension implements BeforeAllCallback {
    private static final DatatypeFactory datatypeFactory;
    private static Class<?> lastTestClass;
    private static int uid;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Сбрасывает uid для нового тестового класса.
     *
     * @return true, если uid сброшен
     */
    public static boolean reset(Object testInstance) {
        Class<?> testClass = testInstance == null ? null : testInstance.getClass();
        if (testClass != null && lastTestClass == testClass) {
            return false;
        }
        lastTestClass = testClass;
        uid = 0;
        return true;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        reset(null);
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов.
     *
     * @return {@link #uid}
     */
    public static int uid() {
        return ++uid;
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов,
     * [0..n-1].
     *
     * @return {@link #uid}
     */
    public static int uid(int n) {
        return uid() % n;
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов.
     *
     * @return {@link #uid}
     */
    public static long uidL() {
        return uid();
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов,
     * [0..n-1].
     *
     * @return {@link #uid}
     */
    public static long uidL(int n) {
        return uid(n);
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов.
     *
     * @return {@link #uid}
     */
    public static double uidD() {
        return uid() + uid(100) / 100.0;
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов,
     * [0..n-1].
     *
     * @return {@link #uid}
     */
    public static double uidD(int n) {
        return uid(n);
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов в
     * виде строки.
     *
     * @return {@link #uid} в виде строки
     */
    public static String uidS() {
        return String.valueOf(uid());
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов, в
     * виде строки [0..n-1]. Дополняет слева нулями.
     *
     * @return {@link #uid}
     */
    public static String uidSLessThan(int n) {
        return StringUtils.leftPad(String.valueOf(uid(n)), (int) Math.log10(n), '0');
    }

    /**
     * Возвращает строковый уникальный идентификатор в рамках запуска тестов, в
     * виде строки, требуемой длины. Дополняет слева нулями.
     * Если требуемая длина меньше текущего уникального идентификатора, то
     * возвращается текущий уникальный идентификатор
     *
     * @param length необходимая длина строкового уникального идентификатора
     * @return уникальной идентификатор необходимой длины
     */
    public static String uidLeftPad(int length) {
        return StringUtils.leftPad(String.valueOf(uid()), length, '0');
    }

    /**
     * Возвращает короткий уникальный идентификатор в рамках запуска тестов.
     *
     * @return {@link #uid}
     */
    public static BigDecimal uidDec() {
        return new BigDecimal(uid() + "." + uid(100)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Возвращает случайное значение перечисления.
     *
     * @param klass    класс перечисления
     * @param excludes исключаемые значения - регулярное выражение или экземпляр перечисления.
     * @return любое значение перечисления
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<?>> T newEnum(Class<T> klass, T... excludes) {
        T[] values = klass.getEnumConstants();
        for (int i = 0; i < values.length; i++) {
            T result = values[uid(values.length)];
            if (!ArrayUtils.contains(excludes, result)) {
                return result;
            }
        }
        throw new IllegalArgumentException(Arrays.toString(excludes));
    }

    /**
     * Возвращает уникальную дату/время.
     *
     * @return date
     */
    public static Date newDate() {
        return newDateWithOffset(MILLIS_PER_DAY);
    }

    /**
     * Возвращает уникальную дату/время.
     *
     * @return instant
     */
    public static Instant newInstant() {
        return newDate().toInstant();
    }

    /**
     * Возвращает уникальную дату.
     *
     * @return date
     */
    public static java.sql.Date newSqlDate() {
        return new java.sql.Date(DateUtils.truncate(newDateWithOffset(MILLIS_PER_DAY), Calendar.DATE).getTime());
    }

    /**
     * Возвращает уникальную дату.
     *
     * @return date
     */
    public static LocalDate newLocalDate() {
        return LocalDate.now().minusDays(uid());
    }

    /**
     * Возвращает уникальную дату/время.
     *
     * @return dateTime
     */
    public static LocalDateTime newLocalDateTime() {
        return LocalDateTime.now().minusDays(uid());
    }

    /**
     * Возвращает уникальную дату/время.
     *
     * @return {@link OffsetDateTime}
     */
    public static OffsetDateTime newOffsetDateTime() {
        return newLocalDateTime().atOffset(ZoneOffset.ofHours(uid(9)));
    }

    private static Date newDateWithOffset(long offset) {
        return new Date(currentTimeMillis() - uid() * offset);
    }

    /**
     * Возвращает уникальную дату/время.
     *
     * @return timestamp
     */
    public static Timestamp newTimestamp() {
        return new Timestamp(currentTimeMillis() - uid() * MILLIS_PER_HOUR);
    }

    public static XMLGregorianCalendar newXMLGregorianCalendar() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(newDate());
        return datatypeFactory.newXMLGregorianCalendar(calendar);
    }
}