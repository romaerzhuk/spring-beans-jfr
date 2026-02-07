package jfr.feign;

import feign.InvocationContext;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.ResponseInterceptor;
import feign.Target;
import jfr.event.FeignRequestEvent;
import jfr.logging.LoggingJoinPoint;
import jfr.logging.NonReentrantLoggingService;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.log.LogMessage;

import static jfr.test.hamcrest.PropertiesMatcher.matching;
import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link JfrFeignRequestInterceptor}.
 *
 * @author Roman_Erzhukov
 */
@ExtendWith({MockitoExtension.class, UidExtension.class})
public class JfrFeignRequestInterceptorTest {
    @InjectMocks
    JfrFeignRequestInterceptor subj;

    @Mock
    NonReentrantLoggingService<FeignRequestEvent> loggingService;

    @Test
    void apply() {
        var template = mock(RequestTemplate.class);
        Target<?> target = mock(Target.class);
        doReturn(target).when(template).feignTarget();
        doReturn(FeignRequestEvent.class).when(target).type();
        String targetUrl = uidS();
        doReturn(targetUrl).when(target).url();
        String method = uidS();
        doReturn(method).when(template).method();
        String templateUrl = uidS();
        doReturn(templateUrl).when(template).url();
        var request = mock(Request.class);
        doReturn(request).when(template).request();
        doAnswer(inv -> {
            LoggingJoinPoint actual = inv.getArgument(0);
            assertThat(actual).is(matching(matcher -> matcher
                    .add("identityPoint", actual.identityPoint(), null)
                    .add("targetClass", actual.targetClass(), FeignRequestEvent.class)
                    .add("name", actual.name(), this, (a, e) -> matcher
                            .add("class", a.getClass(), typeCompatibleWith(LogMessage.class))
                            .add("toString", a.toString(), method + " " + targetUrl + " " + templateUrl))
                    .add("method", actual.method(), this, (a, e) -> matcher
                            .add("class", a.getClass(), typeCompatibleWith(LogMessage.class))
                            .add("toString", a.toString(), request.toString()))
                    .add("args", actual.args(), empty())
            ));
            return null;
        }).when(loggingService).before(any(), any());

        subj.apply(template);

        verify(loggingService).before(any(), isA(FeignRequestEvent.class));
        verifyNoMoreInteractions(loggingService, template, target, request);
    }

    @Test
    @SuppressWarnings({
            "resource",
            "ResultOfMethodCallIgnored"})
    void intercept() throws Exception {
        var context = mock(InvocationContext.class);
        var response = mock(Response.class);
        doReturn(response).when(context).response();
        int status = uid();
        doReturn(status).when(response).status();
        var chain = mock(ResponseInterceptor.Chain.class);
        Object expected = uidS();
        doReturn(expected).when(chain).next(context);

        Object actual = subj.intercept(context, chain);

        assertThat(actual).isEqualTo(expected);
        var inOrder = inOrder(loggingService, context, response, chain);
        inOrder.verify(context).response();
        inOrder.verify(response).status();
        inOrder.verify(loggingService).afterReturning(FeignRequestEvent.class, status);
        inOrder.verify(chain).next(context);
        verifyNoMoreInteractions(loggingService, context, response, chain);
    }
}