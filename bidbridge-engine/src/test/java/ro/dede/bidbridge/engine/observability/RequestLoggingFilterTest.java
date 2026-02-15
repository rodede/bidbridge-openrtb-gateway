package ro.dede.bidbridge.engine.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestLoggingFilterTest {

    @Test
    void addsRequestIdForOpenRtbSubpath() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MetricsCollector(registry);
        var filter = new RequestLoggingFilter(metrics);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/openrtb2/simulator/bid").build()
        );
        filter.filter(exchange, chain -> {
            exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return Mono.empty();
        }).block();

        var requestId = exchange.getResponse().getHeaders().getFirst(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
    }

    @Test
    void skipsNonOpenRtbPaths() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MetricsCollector(registry);
        var filter = new RequestLoggingFilter(metrics);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        filter.filter(exchange, chain -> Mono.empty()).block();

        assertFalse(exchange.getResponse().getHeaders().containsHeader(RequestLoggingFilter.REQUEST_ID_HEADER));
    }
}
