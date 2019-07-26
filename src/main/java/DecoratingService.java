import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;

public class DecoratingService {

    public static void main(String[] args) {
        // build server

        sendRequest();
    }

    private static void sendRequest() {
        final HttpClient client = HttpClient.of("http://127.0.0.1:8080");
        final HttpResponse httpResponse = client.get("/test");
        final AggregatedHttpResponse res = httpResponse.aggregate().join();
        System.err.println(res.headers());
    }
}
