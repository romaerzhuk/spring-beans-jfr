package jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Событие вызова метода Spring-бина, для локализации проблем с производительностью.
 *
 * <p>Первый вызов, корневой, сохраняется в JFR. От событий вложенных вызов сохраняется только статистика и
 * время начала/окончания самого длительного вызова.
 * Из суммарного времени родительских вызовов исключается время вложенных дочерних.</p>
 *
 * @author Roman_Erzhukov
 */
@Category("Spring")
@Name("MethodInvocation")
@Label("Method Invocation")
@StackTrace(false)
public final class MethodInvocationEvent extends AbstractMethodEvent {
}
