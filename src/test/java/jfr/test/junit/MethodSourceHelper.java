package jfr.test.junit;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

/**
 * MethodSourceHelper.
 *
 * @author Roman_Erzhukov
 */
public interface MethodSourceHelper {

    /**
     * Возвращает поток пустых коллекции (null, empty).
     *
     * @return Поток пустых списков
     */
    static Stream<List<?>> emptyLists() {
        return Stream.of(null, List.of());
    }

    /**
     * Возвращает поток пустых строк (null, "").
     *
     * @return Поток пустых строк
     */
    static Stream<String> emptyStrings() {
        return Stream.of(null, "");
    }

    /**
     * Возвращает поток логических значений (false, true).
     *
     * @return Поток логических значений
     */
    static Stream<Boolean> booleans() {
        return Stream.of(false, true);
    }

    /**
     * Возвращает поток из 2 логических значений (false, true).
     *
     * @return Поток логических значений
     */
    static Stream<Arguments> booleans2() {
        return join(booleans(), booleans());
    }

    /**
     * Возвращает поток из 3 логических значений (false, true).
     *
     * @return Поток логических значений
     */
    static Stream<Arguments> booleans3() {
        return join(booleans2(), booleans());
    }

    /**
     * Возвращает поток из 4 логических значений (false, true).
     *
     * @return Поток логических значений
     */
    static Stream<Arguments> booleans4() {
        return join(booleans2(), booleans2());
    }

    /**
     * Возвращает поток из 5 логических значений (false, true).
     *
     * @return Поток логических значений
     */
    static Stream<Arguments> booleans5() {
        return join(booleans3(), booleans2());
    }

    /**
     * Возвращает поток логических значений (false, true, null).
     *
     * @return Поток логических значений
     */
    static Stream<Boolean> booleansWithNull() {
        return Stream.of(false, true, null);
    }

    /**
     * Возвращает перечисления с null-ом.
     *
     * @param enumClass класс перечисления
     * @param <E>       класс перечисления
     * @return перечисления и null
     */
    static <E extends Enum<E>> Stream<E> enumsWithNull(Class<E> enumClass) {
        return Stream.concat(Stream.of((E) null), Stream.of(enumClass.getEnumConstants()));
    }

    /**
     * Возвращает поток значений enum.
     *
     * @return поток значений enum
     */
    static <E extends Enum<E>> Stream<E> streamOf(Class<E> enumClass) {
        return Stream.of(enumClass.getEnumConstants());
    }


    /**
     * Возвращает поток значений enum с дополнительным значением null.
     *
     * @return поток значений enum
     */
    static <E extends Enum<E>> Stream<E> streamWithNull(Class<E> enumClass) {
        return Stream.concat(Stream.of((E) null), Stream.of(enumClass.getEnumConstants()));
    }

    /**
     * Выполняет декартово произведение потоков.
     *
     * @param arguments поток аргументов
     * @param streams   потоки аргументов
     * @return объединённый поток
     */
    static Stream<Arguments> join(Stream<?> arguments, Stream<?>... streams) {
        Stream<Arguments> args = castToArguments(arguments);
        for (Stream<?> stream : streams) {
            List<Arguments> list = castToArguments(stream).toList();
            args = args.flatMap(a -> list.stream()
                    .map(b -> Arguments.of(ArrayUtils.addAll(a.get(), b.get()))));
        }
        return args;
    }

    private static <T> Stream<Arguments> castToArguments(Stream<T> streamToCast) {
        return streamToCast.map(arg ->
                arg instanceof Arguments ? (Arguments) arg : Arguments.of(arg));
    }
}
