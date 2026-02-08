package ro.dede.bidbridge.engine.filters.limits;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.config.EngineLimitsProperties;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InFlightLimitFilterTest {

    @Test
    void rejectsWhenInFlightLimitReached() throws Exception {
        var properties = new EngineLimitsProperties();
        properties.setMaxInFlight(1);
        var registry = new SimpleMeterRegistry();
        var filter = new InFlightLimitFilter(properties, registry);

        var entered = new CountDownLatch(1);
        var firstExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/openrtb2/bid").build()
        );
        WebFilterChain blockingChain = exchange -> Mono.<Void>never()
                .doOnSubscribe(subscription -> entered.countDown());
        Disposable first = filter.filter(firstExchange, blockingChain).subscribe();
        try {
            assertTrue(entered.await(1, TimeUnit.SECONDS));

            var secondExchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/openrtb2/bid").build()
            );
            filter.filter(secondExchange, exchange -> Mono.empty()).block();

            assertEquals(429, secondExchange.getResponse().getStatusCode().value());
            var rejectedCounter = registry.find("engine_rejected_total")
                    .tag("reason", "in_flight_limit")
                    .counter();
            assertEquals(1.0, rejectedCounter == null ? 0.0 : rejectedCounter.count(), 0.0001);
        } finally {
            first.dispose();
        }
    }

    @Test
    void skipsNonOpenRtbPaths() {
        var properties = new EngineLimitsProperties();
        properties.setMaxInFlight(1);
        var registry = new SimpleMeterRegistry();
        var filter = new InFlightLimitFilter(properties, registry);

        var called = new AtomicBoolean(false);
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        filter.filter(exchange, chain -> {
            called.set(true);
            return Mono.empty();
        }).block();

        assertTrue(called.get());
    }
}
