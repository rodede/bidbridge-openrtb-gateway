package ro.dede.bidbridge.engine.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adds a correlation ID and logs request/response timing.
 */
@Component
public class RequestLoggingFilter implements WebFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTR = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final MetricsCollector metrics;

    public RequestLoggingFilter(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var incomingId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        var correlationId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingId;
        exchange.getAttributes().put(CORRELATION_ID_ATTR, correlationId);
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        var timer = metrics.startRequestTimer();
        var start = System.nanoTime();
        return chain.filter(exchange)
                .contextWrite(context -> context.put(CORRELATION_ID_ATTR, correlationId))
                .doFinally(signalType -> {
                    var durationMs = (System.nanoTime() - start) / 1_000_000L;
                    var status = exchange.getResponse().getStatusCode();
                    metrics.recordRequest(status == null ? "unknown" : String.valueOf(status.value()));
                    metrics.stopRequestTimer(timer);
                    log.info("request completed path={} status={} durationMs={}",
                            exchange.getRequest().getPath().value(),
                            status == null ? "unknown" : status.value(),
                            durationMs);
                });
    }
}
