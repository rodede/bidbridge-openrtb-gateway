package ro.dede.bidbridge.engine.adapters.http;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.service.BadBidderResponseException;

import java.util.Map;

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
