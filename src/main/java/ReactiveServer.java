import java.util.Arrays;
import java.util.List;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.ServerCacheControl;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.file.HttpFileBuilder;

class ReactiveServer implements HttpService {

    public static void main(String[] args) {
        final Server server = new ServerBuilder()
                .http(8080)
                .requestTimeoutMillis(0)
                .service("/", HttpFileBuilder.ofResource(ReactiveServer.class.getClassLoader(), "index.html")
                                             .cacheControl(ServerCacheControl.REVALIDATED)
                                             .build()
                                             .asService())
                .service("/pendulum", new ReactiveServer())
                .build();

        server.start().join();
    }

    private static final List<String> frames = Arrays.asList(
            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    │││ \\   ║\n" +
            "║    │││  O  ║\n" +
            "║    OOO     ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║   / │││    ║\n" +
            "║  O  │││    ║\n" +
            "║     OOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>"
    );

    @Override
    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        return HttpResponse.of(200);
    }
}
