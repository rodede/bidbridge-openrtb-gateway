package ro.dede.bidbridge.engine.filters.limits;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.api.OpenRtbConstants;
import ro.dede.bidbridge.engine.config.EngineLimitsProperties;
import ro.dede.bidbridge.engine.observability.MetricsCollector;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

/**
 * Limits in-flight bid requests to avoid overload.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InFlightLimitFilter implements WebFilter {
    private static final String OPENRTB_PREFIX = "/openrtb2";
    private static final byte[] TOO_MANY_BYTES =
            "{\"error\":\"Too many requests\"}".getBytes(StandardCharsets.UTF_8);

    private final Semaphore semaphore;
    private final MetricsCollector metricsCollector;

    public InFlightLimitFilter(EngineLimitsProperties properties, MetricsCollector metricsCollector) {
        var maxInFlight = Math.max(1, properties.getMaxInFlight());
        this.semaphore = new Semaphore(maxInFlight);
        this.metricsCollector = metricsCollector;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var path = exchange.getRequest().getPath().value();
        if (!isLimitedPath(path)) {
            return chain.filter(exchange);
        }
        if (!semaphore.tryAcquire()) {
            metricsCollector.recordInFlightLimitRejection();
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().add(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION);
            var buffer = response.bufferFactory().wrap(TOO_MANY_BYTES);
            return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange)
                .doFinally(signal -> semaphore.release());
    }

    private boolean isLimitedPath(String path) {
        return path != null && path.startsWith(OPENRTB_PREFIX);
    }
}
