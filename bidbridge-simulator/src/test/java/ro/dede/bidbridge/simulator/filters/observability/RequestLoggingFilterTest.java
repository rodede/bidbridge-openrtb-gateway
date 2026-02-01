package ro.dede.bidbridge.simulator.filters.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestLoggingFilterTest {

    @Test
    void recordsMetricsForSuccessfulBid() {
        var registry = new SimpleMeterRegistry();
        var filter = new RequestLoggingFilter(registry);
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/openrtb2/simulator/bid").build()
        );

        filter.filter(exchange, ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }).block();

        var bidCounter = registry.find("sim_requests_total")
                .tag("outcome", "bid")
                .counter();
        assertNotNull(bidCounter);
        assertEquals(1.0, bidCounter.count(), 0.0001);

        var timer = registry.find("sim_latency_ms").timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }
}
