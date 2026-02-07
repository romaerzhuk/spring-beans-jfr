package jfr.event;

import feign.Client;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jfr.logging.LoggingJoinPoint;

import java.util.function.Predicate;

/**
 * Событие вызова Feign-запроса.
 *
 * @author Roman_Erzhukov
 */
@Category("Spring")
@Name("FeignEvent")
@Label("Feign Request")
@StackTrace(false)
public final class FeignRequestEvent extends NonReentrantMethodEvent implements Predicate<LoggingJoinPoint> {
    @Override
    public boolean test(LoggingJoinPoint joinPoint) {
        return Client.class.isAssignableFrom(joinPoint.targetClass());
    }
}
