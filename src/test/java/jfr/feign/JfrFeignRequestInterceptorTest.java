package jfr.feign;

import feign.InvocationContext;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.ResponseInterceptor;
import feign.Target;
import jfr.api.JfrJoinPoint;
import jfr.api.JfrJoinPointFactory;
import jfr.api.NonReentrantLoggingService;
import jfr.event.FeignRequestEvent;
import jfr.test.junit.UidExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.log.LogMessage;

import java.util.List;

import static jfr.test.junit.UidExtension.uid;
import static jfr.test.junit.UidExtension.uidS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    JfrJoinPointFactory joinPointFactory;
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
        var requestName = "request" + uid();
        var request = mock(Request.class, requestName);
        doReturn(request).when(template).request();
        var joinPoint = mock(JfrJoinPoint.class);
        doReturn(joinPoint).when(joinPointFactory).create(any(), any(), any(), any());

        subj.apply(template);

        var name = ArgumentCaptor.forClass(LogMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(LogMessage.class);
        verify(joinPointFactory).create(eq(FeignRequestEvent.class), name.capture(), methodCaptor.capture(), eq(List.of()));
        verify(loggingService).before(eq(joinPoint), isA(FeignRequestEvent.class));
        verify(template, never()).url();
        verify(template, never()).method();
        verify(template, never()).request();
        verifyNoMoreInteractions(joinPointFactory, loggingService, template, target, request);

        assertSoftly(s -> {
            s.assertThat(name.getValue().toString()).as("name").isEqualTo(method + " " + targetUrl + " " + templateUrl);
            s.assertThat(methodCaptor.getValue().toString()).as("method").isEqualTo(requestName);
        });
        verifyNoMoreInteractions(joinPointFactory, loggingService, template, target, request);
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