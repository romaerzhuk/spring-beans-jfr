package jfr.feign;

import feign.InvocationContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.ResponseInterceptor;
import feign.Target;
import jfr.api.JfrJoinPointFactory;
import jfr.api.NonReentrantLoggingService;
import jfr.event.FeignRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.log.LogMessage;

import java.util.List;

/**
 * Перехватывает запросы Feign для записи в Java Flight Recorder.
 *
 * @author Roman_Erzhukov
 */
@RequiredArgsConstructor
public final class JfrFeignRequestInterceptor implements RequestInterceptor, ResponseInterceptor {
    private final JfrJoinPointFactory joinPointFactory;
    private final NonReentrantLoggingService<FeignRequestEvent> loggingService;

    @Override
    public void apply(RequestTemplate template) {
        Target<?> target = template.feignTarget();
        var name = LogMessage.of(() ->
                template.method() + ' ' + target.url() + ' ' + template.url());
        var method = LogMessage.of(() -> template.request()
                .toString());
        loggingService.before(joinPointFactory.create(target.type(), name, method, List.of()), new FeignRequestEvent());
    }

    @Override
    @SuppressWarnings("resource")
    public Object intercept(InvocationContext context, Chain chain) throws Exception {
        Response response = context.response();
        loggingService.afterReturning(FeignRequestEvent.class, response.status());
        return chain.next(context);
    }
}
