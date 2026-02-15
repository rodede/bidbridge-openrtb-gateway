package ro.dede.bidbridge.engine.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
                .doOnEach(signal -> {
                    if (!signal.isOnComplete() && !signal.isOnError()) {
                        return;
                    }
                    var path = exchange.getRequest().getPath().value();
                    if (path != null && path.startsWith("/actuator")) {
                        return;
                    }
                    var durationMs = (System.nanoTime() - start) / 1_000_000L;
                    var status = exchange.getResponse().getStatusCode();
                    var statusValue = status == null ? 0 : status.value();
                    var outcome = resolveOutcome(exchange, statusValue);
                    var requestIdValue = (String) exchange.getAttribute(REQUEST_ID_ATTR);
                    var callerValue = normalizeCaller((String) exchange.getAttribute(CALLER_ATTR));
                    var previousRequestId = MDC.get(REQUEST_ID_ATTR);
                    var previousCaller = MDC.get(CALLER_ATTR);
                    try {
                        if (requestIdValue != null && !requestIdValue.isBlank()) {
                            MDC.put(REQUEST_ID_ATTR, requestIdValue);
                        }
                        MDC.put(CALLER_ATTR, callerValue);
                        log.info("request completed path={} status={} outcome={} caller={} durationMs={}",
                                path,
                                statusValue == 0 ? "unknown" : statusValue,
                                outcome.value(),
                                callerValue,
                                durationMs);
                    } finally {
                        if (previousRequestId == null) {
                            MDC.remove(REQUEST_ID_ATTR);
                        } else {
                            MDC.put(REQUEST_ID_ATTR, previousRequestId);
                        }
                        if (previousCaller == null) {
                            MDC.remove(CALLER_ATTR);
                        } else {
                            MDC.put(CALLER_ATTR, previousCaller);
                        }
                    }
                })
                .doFinally(signalType -> {
                    var status = exchange.getResponse().getStatusCode();
                    var statusValue = status == null ? 0 : status.value();
                    var outcome = resolveOutcome(exchange, statusValue);
                    metrics.recordRequestOutcome(outcome);
                    metrics.stopRequestTimer(timer, outcome);
                });
    }

    private RequestOutcome resolveOutcome(ServerWebExchange exchange, int status) {
        var explicit = exchange.getAttribute(MetricsCollector.ATTR_REQUEST_OUTCOME);
        if (explicit instanceof RequestOutcome requestOutcome) {
            return requestOutcome;
        }
        return RequestOutcome.fromStatus(status);
    }

    private String normalizeCaller(String caller) {
        if (caller == null || caller.isBlank()) {
            return "unknown";
        }
        return caller;
    }
}
