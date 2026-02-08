package ro.dede.bidbridge.engine.adapters.http;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.context.Context;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.observability.RequestLoggingFilter;
import ro.dede.bidbridge.engine.service.BadBidderResponseException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebClientBidderClientTest {

    @Test
    void throwsBadBidderResponseWhenJsonIsInvalid() {
        var server = startServer(200, "application/json", "not-json");
        try {
            var client = new WebClientBidderClient(WebClient.builder(), validator());
            var endpoint = "http://127.0.0.1:" + server.port() + "/openrtb2/bid";

            assertThrows(BadBidderResponseException.class,
                    () -> client.postJson(endpoint, Map.of("id", "req-1")).block());
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void throwsBadBidderResponseWhenResponseFailsValidation() {
        var invalidResponse = """
                {"id":"","seatbid":[{"bid":[{"id":"","impid":"1","price":1.0,"adm":"<vast/>"}]}],"cur":"USD"}
                """;
        var server = startServer(200, "application/json", invalidResponse);
        try {
            var client = new WebClientBidderClient(WebClient.builder(), validator());
            var endpoint = "http://127.0.0.1:" + server.port() + "/openrtb2/bid";

            assertThrows(BadBidderResponseException.class,
                    () -> client.postJson(endpoint, Map.of("id", "req-1")).block());
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void throwsBadBidderResponseWhenStatusIsError() {
        var server = startServer(500, "text/plain", "boom");
        try {
            var client = new WebClientBidderClient(WebClient.builder(), validator());
            var endpoint = "http://127.0.0.1:" + server.port() + "/openrtb2/bid";

            assertThrows(BadBidderResponseException.class,
                    () -> client.postJson(endpoint, Map.of("id", "req-1")).block());
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void forwardsRequestIdAndCallerHeadersFromContext() {
        var capturedRequestId = new AtomicReference<String>();
        var capturedCaller = new AtomicReference<String>();
        var server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> {
                    capturedRequestId.set(request.requestHeaders().get(RequestLoggingFilter.REQUEST_ID_HEADER));
                    capturedCaller.set(request.requestHeaders().get(RequestLoggingFilter.CALLER_HEADER));
                    return response.status(204).send();
                })
                .bindNow();
        try {
            var client = new WebClientBidderClient(WebClient.builder(), validator());
            var endpoint = "http://127.0.0.1:" + server.port() + "/openrtb2/bid";
            client.postJson(endpoint, Map.of("id", "req-1"))
                    .contextWrite(Context.of(
                            RequestLoggingFilter.REQUEST_ID_ATTR, "req-ctx-1",
                            RequestLoggingFilter.CALLER_ATTR, "engine-loadgen"))
                    .block();

            assertEquals("req-ctx-1", capturedRequestId.get());
            assertEquals("engine-loadgen", capturedCaller.get());
        } finally {
            server.disposeNow();
        }
    }

    private DisposableServer startServer(int status, String contentType, String body) {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.status(status)
                        .header("Content-Type", contentType)
                        .sendString(Mono.just(body)))
                .bindNow();
    }

    private jakarta.validation.Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
