package ro.dede.bidbridge.simulator.filters.limits;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.OpenRtbConstants;
import ro.dede.bidbridge.simulator.config.SimulatorLimitsProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InFlightLimitFilterTest {

    @Test
    void returns429WhenInFlightLimitIsReachedForBidPath() {
        var filter = createFilter(1);

        var firstExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post(OpenRtbConstants.OPENRTB_PREFIX + "dsp-a/bid").build()
        );
        Disposable inFlight = filter.filter(firstExchange, ex -> Mono.never()).subscribe();
        try {
            var secondExchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post(OpenRtbConstants.OPENRTB_PREFIX + "dsp-b/bid").build()
            );

            filter.filter(secondExchange, ex -> Mono.empty()).block();

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, secondExchange.getResponse().getStatusCode());
        } finally {
            inFlight.dispose();
        }
    }

    @Test
    void bypassesLimitForActuatorPath() {
        var filter = createFilter(1);

        var inFlightBid = MockServerWebExchange.from(
                MockServerHttpRequest.post(OpenRtbConstants.OPENRTB_PREFIX + "dsp-a/bid").build()
        );
        Disposable inFlight = filter.filter(inFlightBid, ex -> Mono.never()).subscribe();
        try {
            var healthExchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/actuator/health").build()
            );

            filter.filter(healthExchange, ex -> {
                ex.getResponse().setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }).block();

            assertEquals(HttpStatus.OK, healthExchange.getResponse().getStatusCode());
        } finally {
            inFlight.dispose();
        }
    }

    private InFlightLimitFilter createFilter(int maxInFlight) {
        var properties = new SimulatorLimitsProperties();
        properties.setMaxInFlight(maxInFlight);
        return new InFlightLimitFilter(properties, new SimpleMeterRegistry());
    }
}
