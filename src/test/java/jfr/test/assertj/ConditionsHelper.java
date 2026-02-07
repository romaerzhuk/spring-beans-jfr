package jfr.test.assertj;

import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.condition.NestableCondition;
import org.assertj.core.condition.VerboseCondition;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.function.Function;

import static org.hamcrest.Matchers.is;

/**
 * Вспомогательные условия дял {@link NestableCondition}.
 *
 * @author Roman_Erzhukov
 */
public final class ConditionsHelper {
    /**
     * Возвращает ленивое условие проверки.
     *
     * @param isExpectedNull признак того, что ожидаемое значение null
     * @param callback       создаёт проверку по актуальному значению
     * @param <T>            тип значения
     * @return условие проверки
     */
    public static <T> Condition<T> mapperCondition(boolean isExpectedNull, Function<T, Condition<T>> callback) {
        return isExpectedNull ? nullValue() : lazyCondition(callback);
    }

    /**
     * Возврщаает ленивое условие проверки.
     *
     * @param callback создаёт проверку по актуальному значению
     * @param <T>      тип значения
     * @return условие проверки
     */
    public static <T> Condition<T> lazyCondition(Function<T, Condition<T>> callback) {
        return new LazyCondition<>(callback);
    }

    /**
     * Возвращает уловие проверки {@link Condition} поля DTO/Entity на equals.
     *
     * @param name     описание
     * @param actual   актуальное значение поля
     * @param expected ожидаемое значение поля
     * @param <T>      тип DTO/Entity
     * @param <R>      тип поля DTO/Entity
     * @return проверка значения поля на equals
     */
    public static <T, R> Condition<T> isEqual(String name, R actual, Object expected) {
        return match(name, actual, is(expected));
    }

    /**
     * Возвращает условие проверки {@link Condition} поля DTO/Enitity.
     *
     * @param name    описание
     * @param actual  актуальное значение поля
     * @param matcher ожидаемое значение поля
     * @param <T>     тип DTO/Entity
     * @param <R>     тип поля DTO/Entity
     * @return проверка значения поля
     */
    public static <T, R> Condition<T> match(String name, R actual, Matcher<?> matcher) {
        return VerboseCondition.verboseCondition(value -> matcher.matches(actual),
                name, value -> ": expected %s but actual is %s".formatted(matcher, actual));
    }

    /**
     * Возвращает условие проверки на null.
     *
     * @param <T> тип значения
     * @return проверка на null
     */
    @SuppressWarnings("unchecked")
    public static <T> Condition<T> nullValue() {
        return HamcrestCondition.matching((Matcher<T>) Matchers.nullValue());
    }
}
