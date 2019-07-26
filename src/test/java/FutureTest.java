import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

class FutureTest {

    @Test
    void futureCallback() {
        System.err.println("Thread: " + Thread.currentThread().getName());

        final CompletableFuture<Void> future = new CompletableFuture<>();
        final Thread thread = new Thread(() -> future.complete(null));
        thread.setName("t1");
        thread.start();

        future.handle((unused, cause) -> {
            System.err.println("In handle. Thread: " + Thread.currentThread().getName());
            return null;
        });
    }
}
