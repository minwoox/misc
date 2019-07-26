import java.util.concurrent.CompletableFuture;

import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;

public class RequestContextPropagation {

    public static void main(String[] args) {
        final Server backend1 = new ServerBuilder()
                .http(8081)
                .route()
                .get("/api")
                .build((ctx, req) -> HttpResponse.of(200))
                .build();
        backend1.start().join();

        final Server backend2 = new ServerBuilder()
                .http(8082)
                .route()
                .get("/api")
                .build((ctx, req) -> HttpResponse.of(200))
                .build();
        backend2.start().join();

        final Server server = new ServerBuilder()
                .http(8080)
                .route()
                .get("/service")
                .build(new HttpService() {
                    @Override
                    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                        return HttpResponse.of();
                    }
                }).build();
        server.start().join();

        sendRequest();
    }

    private static void sendRequest() {
        final HttpClient client = HttpClient.of("http://127.0.0.1:8080");
        final HttpResponse httpResponse = client.get("/service");
        final AggregatedHttpResponse res = httpResponse.aggregate().join();
        System.err.println(res.headers());
    }

}
