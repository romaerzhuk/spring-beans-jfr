package jfr.test.assertj;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;

import java.util.function.Function;

/**
 * Ленивые условия проверки.
 *
 * @author Ержуков Роман
 */
@RequiredArgsConstructor
class LazyCondition<T> extends Condition<T> {
    private final Function<T, Condition<T>> callback;

    private T actual;
    private Condition<T> delegate;

    @Override
    public boolean matches(T actual) {
        this.actual = actual;
        delegate = callback.apply(actual);
        return delegate.matches(actual);
    }

    @Override
    public Description conditionDescriptionWithStatus(T actual) {
        return delegate == null ? super.conditionDescriptionWithStatus(actual)
                : delegate.conditionDescriptionWithStatus(actual);
    }

    @Override
    public Description description() {
        return delegate == null ? super.description() : delegate.conditionDescriptionWithStatus(actual);
    }
}
