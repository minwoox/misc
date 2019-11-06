# Reactive Streams, Event Loop and Connection pooling with Armeria client

### Reactive Streams usage in Armeria

- What is Reactive Streams?
  - An initiative to provide a standard for asynchronous stream processing with non-blocking back pressure.
  - [Specification for the JVM](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/README.md)
  - `Publisher`, `Subscriber`, `Subscription` and `Processor`
  - Simplified subscription flow
    - `Subscriber` subscribes the `Publisher` with `Publisher.subsribe(Subscriber)`.
    - `Publisher` creates a `Subscription`.
    - `Subscriber.onSubscribe(Subscription)` is called.
    - `Subscription.request(demands)` is called in the `Subscriber` code.
    - `Subscriber.onNext(element)` to pass data to the `Subscriber`.
    - `Subscriber.onComplete()` or `Subscriber.onError(throwable)` is called depending on the situation.
- Reactive Streams implementations in Armeria
  - `Publisher` (`StreamMessage`)
    - `HttpRequest` and `HttpResponse` extends it
    - `DefaultStreamMessage`
      - Queue based
    - `AbstractStreamMessageDuplicator`
      - What if we want the elements from multiple places?
    - `FixedStreamMessage`
      - `EmptyFixedStreamMessage`
      - `OneElementFixedStreamMessage`
      - `TwoElementFixedStreamMessage`
    - `FilteredStreamMessage`
      - What if we want to add some header in the decorator?
    - `DeferredStreamMessage`
      - Deferred?
  - Let's implement our own subscriber.
  - `Subscriber`
    - `HttpMessageAggregator`
      - `HttpClient.execute(...)` returns an `HttpResponse`.
      - You can aggregate the `HttpResponse` using `HttpResponse.aggregate()`.
      - Then, it will return a `CompletableFuture<AggregatedHttpResponse>`.
      - In `HttpResponse.aggregate()`, `HttpMessageAggregator` which is a `Subscriber`, subscribes
      the `StreamMessage` and it collects all the response.
    - `HttpRequestSubscriber`
      - `HttpClient` sends an `HttpRequest` when calling `execute(...)`, `get()` and so on.
      - `HttpRequestSubscriber` subscribes the `StreamMessage`.
        - It writes the header first.
        - If the write was successful, `operationComplete` is called which is a method of `ChannelFutureListener`.
        - Then, it calls `subscription.request(1)`.
        - `onNext(HttpObject)` is called, and it writes the object and it goes on and on until
        it consumes all the elements.
      - `Http1ResponseDecoder` implements `ChannelInboundHandler`.
    - `HttpResponseSubscriber` is used by the server side.
    - Let's build a simple decorating service.
  - The difference between `HttpRequest` and `HttpResponse`
  
### Event Loop

- Who does the all jobs above?
- What is the event loop?
  - A general term which waits for and dispatches events or messages in a program.
  - We use `EventLoop` in Netty.
    - Handles all the I/O operations for a `Channel` once registered.
    - One `EventLoop` instance usually handles more than one `Channel` but this may depend on implementation details and internals.
    - `EventLoop` extends Java's `ScheduledExecutorService`.
    - Events and Tasks are executed in order received.
    - Let's see what it really does with [`EpollEventLoop`](https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/nio/NioEventLoop.java#L445)
  - From the point of view of the `EventLoop`s   
- How many `EventLoop`s ?
  - No official formula
    - Nthreads = Ncpu * Ucpu * (1 + W/C)
- Let's build a simple reactive server.
    
### Request flow

- Sending an `HttpRequest`
  - `HttpClient` -> `UserClient` -> HTTP decorators -> `HttpClientDelegate` -> `HttpSessionHandler`
  - Creates a `ClientRequestContext` in `UserClient`
  - Brings an `EventLoop`.
    - [`EventLoopScheduler.acquire()`](https://github.com/line/armeria/blob/0296b6cb71945cf0871ac957e896fe95b8c64151/core/src/main/java/com/linecorp/armeria/client/EventLoopScheduler.java#L54)
    - Stores all the `EventLoop`s in the `Map` whose key is `Endpoint.authority()`
    - `EventLoop`s are managed in a binary heap, using active request count and `eventloop` id.
  - The `EventLoop` is [used to subscribe](https://github.com/line/armeria/blob/bc8abec3d0a3f1d52746643372b1dbabe5bf853c/core/src/main/java/com/linecorp/armeria/client/HttpSessionHandler.java#L145)
    - So the thread who calls `HttpClient.execute()` and write to the `Channel` can be different.
- Receiving an `HttpResponse`
  - The thread who writes to the response is the `EventLoop` you used when sending the `Request`.
- Thrift client
  - `HelloSerivce.Iface()` or `AsyncIface()` -> `THttpClientInvocationHandler` -> `DefaultTHttpClient(UserClient)` ->
  RPC decorators -> `THttpClientDelegate` -> HTTP decorators -> `HttpClientDelegate`
  
### `RequestContext`

- Thread-local storage
  - `makeContextAware`

### Connection pooling

- How many connections we have?
  - HTTP/1.1
    - What's the [pipelining](https://en.wikipedia.org/wiki/HTTP_pipelining)?
    - [In Armeria](https://github.com/line/armeria/blob/armeria-0.88.0/core/src/main/java/com/linecorp/armeria/client/HttpClientDelegate.java#L224)
  - HTTP/2
    - Why is it possible just to have one connection?
      - What Content-length header is for?
    - [Frame format](https://tools.ietf.org/html/rfc7540#section-4.1)
  
- Creates a [`PoolKey`](https://github.com/line/armeria/blob/d90aea4704982df06251c1132bbc4da33301725d/core/src/main/java/com/linecorp/armeria/client/HttpClientDelegate.java#L104) with host, ip, port and session protocol.
- Gets a [`KeyedChannelPool`](https://github.com/line/armeria/blob/d90aea4704982df06251c1132bbc4da33301725d/core/src/main/java/com/linecorp/armeria/client/HttpClientFactory.java#L289) using the `EventLoop`.

### Let's build a proxy server