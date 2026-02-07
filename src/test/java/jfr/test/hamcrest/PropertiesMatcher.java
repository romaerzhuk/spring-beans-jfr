package jfr.test.hamcrest;

import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.is;

/**
 * Сравнивает объект по его свойствам.
 *
 * @param <T> тип объекта
 * @author Roman_Erzhukov
 * @see HamcrestCondition
 */
public class PropertiesMatcher<T> extends BaseMatcher<T> {
    private static class FieldName {
        final String name;
        int index = -1;

        FieldName(String name) {
            this.name = name;
        }
    }

    private final List<FieldName> fieldName = new ArrayList<>();
    private final String name;
    private final BiConsumer<T, PropertiesMatcher<?>> consumer;
    private List<String> list;
    private StringBuilder sb;

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    public static <T> Matcher<T> of(Consumer<PropertiesMatcher<?>> consumer) {
        return of("", biConsumer(consumer));
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param name     имя эталонного объекта
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    public static <T> Matcher<T> of(String name, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return new PropertiesMatcher<>(name, consumer);
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param name     имя эталонного объекта
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    public static <T> Matcher<T> of(Class<T> name, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return of(name.getSimpleName(), consumer);
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param expected если expected равен null, то возвращается {@link Matchers#nullValue()}
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    public static <T> Matcher<T> of(Object expected, Consumer<PropertiesMatcher<?>> consumer) {
        return of("", expected, biConsumer(consumer));
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param name     имя эталонного объекта
     * @param expected если expected равен null, то возвращается {@link Matchers#nullValue()}
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    public static <T> Matcher<T> of(Class<T> name, Object expected, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return of(name.getSimpleName(), expected, consumer);
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param name     имя эталонного объекта
     * @param expected если expected равен null, то возвращается {@link Matchers#nullValue()}
     * @param consumer метод сравнения объектов
     * @return {@link Matcher}
     */
    @SuppressWarnings("unchecked")
    public static <T> Matcher<T> of(String name, Object expected, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return expected == null ? (Matcher<T>) Matchers.nullValue() : of(name, consumer);
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(Consumer<PropertiesMatcher<?>> consumer) {
        return matching("", consumer);
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param name     condition name
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(Class<T> name, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return matching(of(name, consumer));
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param name     condition name
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(String name, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return matching(of(name, consumer));
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param expected if expected is null returns null condition
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(Object expected, Consumer<PropertiesMatcher<?>> consumer) {
        return matching(of(expected, consumer));
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param name     condition name
     * @param expected if expected is null returns null condition
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(Class<T> name, Object expected, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return matching(of(name.getSimpleName(), expected, consumer));
    }

    /**
     * Constructs a {@link Condition} using the matcher given as a parameter.
     *
     * @param <T>      the type the condition is about
     * @param name     condition name
     * @param expected if expected is null returns null condition
     * @param consumer the matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(String name, Object expected, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        return matching(of(name, expected, consumer));
    }

    /**
     * Создаёт эталонный объект для сравнения.
     *
     * @param <T>     the type the condition is about
     * @param matcher the Hamcrest matcher to use as a condition
     * @return the built {@link Condition}
     */
    public static <T> Condition<T> matching(Matcher<? extends T> matcher) {
        return new Condition<>(matcher::matches, "%s", matcher);
    }

    private static <T> BiConsumer<T, PropertiesMatcher<?>> biConsumer(Consumer<PropertiesMatcher<?>> consumer) {
        return (actual, matcher) ->
                consumer.accept(matcher);
    }

    private PropertiesMatcher(String name, BiConsumer<T, PropertiesMatcher<?>> consumer) {
        this.name = name;
        this.consumer = consumer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean matches(Object item) {
        list = null;
        try {
            consumer.accept((T) item, this);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return isEqual();
    }

    @Override
    public void describeTo(Description d) {
        d.appendText(name + (list != null ? list : ""));
    }

    /**
     * Проверяет свойства объекта на совпадение.
     *
     * @param name     имя свойства
     * @param actual   актуальное значение
     * @param expected ожидаемое значение, или {@link Matcher}
     * @return this
     */
    public PropertiesMatcher<T> add(String name, Object actual, Object expected) {
        if (expected instanceof Matcher) {
            return test(name, actual, (Matcher<?>) expected);
        }
        return test(name, actual, is(expected));
    }

    /**
     * Проверяет свойства объекта на совпадение с проверкой на null.
     *
     * @param <A>      тип актуального значения
     * @param <E>      тип ожидаемого значения
     * @param name     наименование свойства
     * @param actual   актуальное значение
     * @param expected ожидаемое значение
     * @param consumer проверка для не null-значений
     * @return this
     */
    public <A, E> PropertiesMatcher<T> add(String name, A actual, E expected, BiConsumer<A, E> consumer) {
        if (actual == null || expected == null) {
            add(name + " is null", actual == null, expected == null);
            return this;
        }
        FieldName field = new FieldName(name);
        fieldName.add(field);
        consumer.accept(actual, expected);
        fieldName.remove(fieldName.size() - 1);
        return this;
    }

    /**
     * Проверяет список-свойство объекта на совпадение.
     *
     * @param name       имя спика
     * @param actual     актуальное множество
     * @param comparator сортирует элементы множество
     * @param expected   ожидаемый список
     * @param callback   проверка элемента списка
     * @param <A>        тип актуального элемента
     * @param <E>        тип ожидаемого элемента
     * @return this
     */
    public <A, E> PropertiesMatcher<T> addList(String name, Iterable<A> actual, Comparator<A> comparator, List<E> expected,
                                               BiConsumer<A, E> callback) {
        List<A> actualList = actual == null ? null : StreamSupport.stream(actual.spliterator(), false)
                .sorted(comparator)
                .collect(Collectors.toList());
        return addList(name, actualList, expected, callback);
    }

    /**
     * Проверяет список-свойство объекта на совпадение.
     *
     * @param name       имя спика
     * @param actual     актуальное множество
     * @param expected   ожидаемый список
     * @param comparator сортирует элементы множество
     * @param callback   проверка элемента списка
     * @param <A>        тип актуального элемента
     * @param <E>        тип ожидаемого элемента
     * @return this
     */
    public <A, E> PropertiesMatcher<T> addList(String name, List<A> actual, Iterable<E> expected, Comparator<E> comparator,
                                               BiConsumer<A, E> callback) {
        List<E> expectedList = expected == null ? null : StreamSupport.stream(expected.spliterator(), false)
                .sorted(comparator)
                .collect(Collectors.toList());
        return addList(name, actual, expectedList, callback);
    }

    /**
     * Проверяет список-свойство объекта на совпадение.
     *
     * @param name     имя спика
     * @param actual   актуальный список
     * @param expected ожидаемый список
     * @param callback проверка элемента списка
     * @param <A>      тип актуального элемента
     * @param <E>      тип ожидаемого элемента
     * @return this
     */
    public <A, E> PropertiesMatcher<T> addList(String name, List<A> actual, List<E> expected, BiConsumer<A, E> callback) {
        if (actual == null && expected == null) {
            return this;
        }
        Integer actualSize = actual == null ? null : actual.size();
        Integer expectedSize = expected == null ? null : expected.size();
        if (!Objects.equals(actualSize, expectedSize)) {
            add(name + ".size", actualSize, expectedSize);
            return this;
        }
        FieldName field = new FieldName(name);
        fieldName.add(field);
        for (int i = 0; i < expected.size(); i++) {
            field.index = i;
            callback.accept(actual.get(i), expected.get(i));
        }
        fieldName.remove(fieldName.size() - 1);
        return this;
    }

    private PropertiesMatcher<T> test(String name, Object actual, Matcher<?> matcher) {
        if (!matcher.matches(actual)) {
            if (list == null) {
                list = new ArrayList<>();
                sb = new StringBuilder();
            }
            sb.append('{');
            String splitter = "";
            for (FieldName field : fieldName) {
                sb.append(splitter).append(field.name);
                if (field.index >= 0) {
                    sb.append('[').append(field.index).append(']');
                }
                splitter = ".";
            }
            sb.append(splitter).append(name).append(": ").append(actual);
            StringDescription description = new StringDescription(sb);
            sb.append(' ');
            matcher.describeTo(description);
            sb.append('}');
            list.add(sb.toString());
            sb.setLength(0);
        }
        return this;
    }

    /**
     * Возвращает признак равенства объектов.
     *
     * @return true, если объекты равны
     */
    public boolean isEqual() {
        return list == null;
    }
}
