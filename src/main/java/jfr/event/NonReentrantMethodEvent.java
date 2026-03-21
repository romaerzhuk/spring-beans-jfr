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
 * <p>Для некоторых событий, в частности {@link FeignRequestEvent}, не удалось воспользоваться этими вызовами.
 * Пришлось пожертвовать возможностью повторного вхождения в метод и исключить
 * {@link NonReentrantMethodEvent} из подсчёта статистики времени выполнения вложенных вызовов: в JFR пишется каждое событие.</p>
 *
 * @author Roman_Erzhukov
 */
public abstract class NonReentrantMethodEvent extends AbstractMethodEvent {
}
