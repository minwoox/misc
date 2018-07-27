import java.util.concurrent.CompletableFuture;

import org.junit.ClassRule;
import org.junit.Test;

import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.HttpClientBuilder;
import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.server.ServerRule;

public class MyTest {

    @ClassRule
    public static ServerRule backend = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", new AbstractHttpService() {
                @Override
                protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                    final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                    req.aggregate().whenComplete((msg, cause) -> {
                        future.complete(HttpResponse.of("hello~! This message is from the backend server."));
                    });
                    return HttpResponse.from(future);
                }
            });
        }
    };

    @ClassRule
    public static ServerRule apiServer = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", new AbstractHttpService() {
                @Override
                protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                    final HttpResponseWriter streaming = HttpResponse.streaming();
                    req.aggregate().whenComplete((msg, cause) -> {
                        streaming.write(HttpHeaders.of(200));
                        streaming.write(HttpData.ofUtf8("hello~! This message is from the api server.\n"));
                        final HttpClient client = new HttpClientBuilder(backend.uri("/")).build();

                        client.get("/").aggregate().whenComplete((backendMsg, backendCause) -> {
                            streaming.write(backendMsg.content());
                            streaming.close();
                        });
                    });
                    return streaming;
                }
            });
        }
    };



    @Test
    public void futureCallback() {
        System.err.println("Hi. I am " + Thread.currentThread().getName() + " thread.");

        final CompletableFuture<Object> future = new CompletableFuture<>();
        final Thread thread = new Thread(() -> future.complete(new Object()));
        thread.setName("minwoo");

        future.whenComplete(((object, cause) -> {
            System.err.println("I am executed by " + Thread.currentThread().getName() + " thread.");
        }));

        thread.start();
    }

    @Test
    public void clientTest() {
        final HttpClient client = new HttpClientBuilder(apiServer.uri("/")).build();
        final AggregatedHttpMessage httpMessage = client.get("/").aggregate().join();
        System.err.println(httpMessage.content().toStringUtf8());
    }
}
