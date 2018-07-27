import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

public class WithEventLoop {

    @ClassRule
    public static ServerRule backend = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", new AbstractHttpService() {
                @Override
                protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                    System.err.println("I am executed by " + Thread.currentThread().getName() +
                                       " thread in backend #1.");
                    final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                    req.aggregate().whenComplete((msg, cause) -> {
                        future.complete(HttpResponse.of("hello~! This message is from the backend server."));
                        System.err.println("I am executed by " + Thread.currentThread().getName() +
                                           " thread in backend #2.");
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
                    System.err.println("I am executed by " + Thread.currentThread().getName() +
                                       " thread in api server #1.");
                    final HttpResponseWriter streaming = HttpResponse.streaming();
                    req.aggregate().whenComplete((msg, cause) -> {
                        System.err.println("I am executed by " + Thread.currentThread().getName() +
                                           " thread in api server #2.");
                        streaming.write(HttpHeaders.of(200));
                        streaming.write(HttpData.ofUtf8("hello~! This message is from the api server.\n"));

                        final HttpClient client = new HttpClientBuilder(backend.uri("/")).build();

                        client.get("/").aggregate().whenComplete((backendMsg, backendCause) -> {
                            System.err.println("I am executed by " + Thread.currentThread().getName() +
                                               " thread in api server #3.");
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
    public void clientTest() {
        final HttpClient client = new HttpClientBuilder(apiServer.uri("/")).build();
        final CompletableFuture<AggregatedHttpMessage> future = client.get("/").aggregate();

        future.whenComplete((msg, cause) -> {
            System.err.println(msg.content().toStringUtf8());
            System.err.println("I am executed by " + Thread.currentThread().getName() +
                               " thread in client.");
        });

        await().untilAsserted(() -> assertThat(future.isDone()).isTrue());
    }
}

