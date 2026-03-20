package jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Событие вызова Feign-запроса.
 *
 * @author Roman_Erzhukov
 */
@Category("Spring")
@Name("FeignEvent")
@Label("Feign Request")
@StackTrace(false)
public final class FeignRequestEvent extends NonReentrantMethodEvent {
}
