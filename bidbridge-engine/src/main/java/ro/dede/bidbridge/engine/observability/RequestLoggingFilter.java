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
 * Adds a request ID and logs request/response timing.
 */
@Component
public class RequestLoggingFilter implements WebFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CALLER_HEADER = "X-Caller";
    public static final String REQUEST_ID_ATTR = "requestId";
    public static final String CALLER_ATTR = "caller";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final MetricsCollector metrics;

    public RequestLoggingFilter(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var incomingId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        var requestId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingId;
        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        var caller = exchange.getRequest().getHeaders().getFirst(CALLER_HEADER);
        if (caller != null && !caller.isBlank()) {
            exchange.getAttributes().put(CALLER_ATTR, caller);
            exchange.getResponse().getHeaders().add(CALLER_HEADER, caller);
        }

        var timer = metrics.startRequestTimer();
        var start = System.nanoTime();
        return chain.filter(exchange)
                .contextWrite(context -> {
                    var updated = context.put(REQUEST_ID_ATTR, requestId);
                    if (caller != null && !caller.isBlank()) {
                        updated = updated.put(CALLER_ATTR, caller);
                    }
                    return updated;
                })
                .doFinally(signalType -> {
                    var durationMs = (System.nanoTime() - start) / 1_000_000L;
                    var status = exchange.getResponse().getStatusCode();
                    var statusValue = status == null ? 0 : status.value();
                    var outcome = outcomeForStatus(statusValue);
                    var callerValue = (String) exchange.getAttribute(CALLER_ATTR);
                    metrics.recordRequestOutcome(outcome);
                    metrics.stopRequestTimer(timer, outcome);
                    log.info("request completed path={} status={} caller={} durationMs={}",
                            exchange.getRequest().getPath().value(),
                            status == null ? "unknown" : status.value(),
                            callerValue == null ? "" : callerValue,
                            durationMs);
                });
    }

    private String outcomeForStatus(int status) {
        return switch (status) {
            case 200 -> "bid";
            case 204 -> "nobid";
            case 0 -> "unknown";
            default -> "error";
        };
    }
}
