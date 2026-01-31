package ro.dede.bidbridge.simulator.limits;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.config.SimulatorLimitsProperties;
import ro.dede.bidbridge.simulator.observability.RequestLogEnricher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

/**
 * Limits in-flight requests to avoid overwhelming the simulator.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InFlightLimitFilter implements WebFilter {
    private static final String OPENRTB_VERSION_HEADER = "X-OpenRTB-Version";
    private static final String OPENRTB_VERSION = "2.6";
    private static final byte[] TOO_MANY_BYTES =
            "{\"error\":\"Too many requests\"}".getBytes(StandardCharsets.UTF_8);

    private final Semaphore semaphore;
    private final RequestLogEnricher logEnricher = new RequestLogEnricher();
    private final Counter rejectedCounter;

    public InFlightLimitFilter(SimulatorLimitsProperties properties, MeterRegistry meterRegistry) {
        var maxInFlight = Math.max(1, properties.getMaxInFlight());
        this.semaphore = new Semaphore(maxInFlight);
        this.rejectedCounter = Counter.builder("sim_rejected_total")
                .tag("reason", "in_flight_limit")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!semaphore.tryAcquire()) {
            rejectedCounter.increment();
            logEnricher.captureError(exchange, "too_many_requests", "Too many requests");
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().add(OPENRTB_VERSION_HEADER, OPENRTB_VERSION);
            var buffer = response.bufferFactory().wrap(TOO_MANY_BYTES);
            return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange)
                .doFinally(signal -> semaphore.release());
    }
}
