package ro.dede.bidbridge.simulator.filters.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import ro.dede.bidbridge.simulator.OpenRtbConstants;
import ro.dede.bidbridge.simulator.observability.RequestLogAttributes;

import java.util.UUID;

/**
 * Adds request correlation headers and logs one summary line per request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements WebFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CALLER_HEADER = "X-Caller";
    public static final String REQUEST_ID_ATTR = "requestId";
    public static final String CALLER_ATTR = "caller";
    public static final String START_NANOS_ATTR = "startNanos";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String OUTCOME_BID = "bid";
    private static final String OUTCOME_NOBID = "nobid";
    private static final String OUTCOME_ERROR = "error";

    private final Timer latencyTimer;
    private final Counter bidCounter;
    private final Counter noBidCounter;
    private final Counter errorCounter;

    public RequestLoggingFilter(MeterRegistry meterRegistry) {
        this.latencyTimer = Timer.builder("sim_latency_ms")
                .description("Simulator request latency in milliseconds")
                .register(meterRegistry);
        this.bidCounter = Counter.builder("sim_requests_total")
                .tag("outcome", OUTCOME_BID)
                .register(meterRegistry);
        this.noBidCounter = Counter.builder("sim_requests_total")
                .tag("outcome", OUTCOME_NOBID)
                .register(meterRegistry);
        this.errorCounter = Counter.builder("sim_requests_total")
                .tag("outcome", OUTCOME_ERROR)
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var incomingRequestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        var requestId = (incomingRequestId == null || incomingRequestId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingRequestId;
        var caller = exchange.getRequest().getHeaders().getFirst(CALLER_HEADER);
        var startNanos = System.nanoTime();

        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);
        if (caller != null && !caller.isBlank()) {
            exchange.getAttributes().put(CALLER_ATTR, caller);
        }
        exchange.getAttributes().put(START_NANOS_ATTR, startNanos);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        return chain.filter(exchange)
                .contextWrite(ctx -> withContext(ctx, requestId, caller))
                .doFinally(signal -> logSummary(exchange));
    }

    private Context withContext(Context context, String requestId, String caller) {
        var updated = context.put(REQUEST_ID_ATTR, requestId);
        if (caller != null && !caller.isBlank()) {
            updated = updated.put(CALLER_ATTR, caller);
        }
        return updated;
    }

    private void logSummary(ServerWebExchange exchange) {
        var path = exchange.getRequest().getPath().value();
        if (!isBusinessPath(path)) {
            return;
        }
        var status = exchange.getResponse().getStatusCode();
        var statusValue = status == null ? 0 : status.value();
        var requestId = (String) exchange.getAttribute(REQUEST_ID_ATTR);
        var caller = (String) exchange.getAttribute(CALLER_ATTR);
        var startNanos = (Long) exchange.getAttribute(START_NANOS_ATTR);
        var durationMs = startNanos == null ? 0L : (System.nanoTime() - startNanos) / 1_000_000L;
        var dspId = (String) exchange.getAttribute(RequestLogAttributes.DSP_ID);
        var latencyMs = exchange.getAttribute(RequestLogAttributes.LATENCY_MS);
        var errorType = (String) exchange.getAttribute(RequestLogAttributes.ERROR_TYPE);
        var errorMessage = (String) exchange.getAttribute(RequestLogAttributes.ERROR_MESSAGE);
        var outcome = outcomeForStatus(statusValue);

        var mdc = mdc(requestId, caller);
        try {
            log.info("requestId={} caller={} path={} status={} dspId={} latencyMs={} durationMs={} errorType={} errorMessage={}",
                    requestId,
                    caller == null ? "" : caller,
                    path,
                    statusValue,
                    dspId,
                    latencyMs,
                    durationMs,
                    statusValue >= 400 ? errorType : "",
                    statusValue >= 400 ? errorMessage : "");
            latencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            incrementOutcome(outcome);
        } finally {
            mdc.close();
        }
    }

    private void incrementOutcome(String outcome) {
        switch (outcome) {
            case OUTCOME_BID -> bidCounter.increment();
            case OUTCOME_NOBID -> noBidCounter.increment();
            default -> errorCounter.increment();
        }
    }

    private String outcomeForStatus(int statusValue) {
        return switch (statusValue) {
            case 200 -> OUTCOME_BID;
            case 204 -> OUTCOME_NOBID;
            default -> OUTCOME_ERROR;
        };
    }

    private boolean isBusinessPath(String path) {
        return path != null && path.startsWith(OpenRtbConstants.OPENRTB_PREFIX);
    }

    private MdcScope mdc(String requestId, String caller) {
        var previousRequestId = MDC.get(REQUEST_ID_ATTR);
        var previousCaller = MDC.get(CALLER_ATTR);
        if (requestId != null) {
            MDC.put(REQUEST_ID_ATTR, requestId);
        }
        if (caller != null && !caller.isBlank()) {
            MDC.put(CALLER_ATTR, caller);
        }
        return new MdcScope(previousRequestId, previousCaller);
    }

    private void resetMdc(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previous);
        }
    }

    private final class MdcScope implements AutoCloseable {
        private final String previousRequestId;
        private final String previousCaller;

        private MdcScope(String previousRequestId, String previousCaller) {
            this.previousRequestId = previousRequestId;
            this.previousCaller = previousCaller;
        }

        @Override
        public void close() {
            resetMdc(REQUEST_ID_ATTR, previousRequestId);
            resetMdc(CALLER_ATTR, previousCaller);
        }
    }
}
