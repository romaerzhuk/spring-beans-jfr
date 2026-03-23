package jfr.event;

import jfr.api.JfrJoinPoint;
import jfr.api.JfrLoggingService;
import jfr.api.JoinPointCallback;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Метод не допускающий вложенные вызовы.
 *
 * <p>{@link JfrLoggingService#proceed(ProceedingJoinPoint)} и {@link JfrLoggingService#proceedCallback(JfrJoinPoint, JoinPointCallback)}
 * обеспечивают гарантию завершающих вызов внутри своей реализации.</p>
 *
 * <p>Для {@link FeignRequestEvent}, не удалось воспользоваться этими вызовами. Пришлось пожертвовать возможностью повторного вхождения в метод.</p>
 *
 * @author Roman_Erzhukov
 */
public abstract class NonReentrantMethodEvent extends AbstractMethodEvent {
    @Override
    public final boolean isReentrant() {
        return false;
    }
}
