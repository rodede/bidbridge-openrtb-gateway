package ro.dede.bidbridge.engine.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Centralized metrics helper for requests and adapter outcomes.
 */
@Component
public class MetricsCollector {
    public static final String ATTR_REQUEST_OUTCOME = "requestOutcome";

    public static final String METRIC_REQUESTS_TOTAL = "requests_total";
    public static final String METRIC_ERRORS_TOTAL = "errors_total";
    public static final String METRIC_ADAPTER_TIMEOUTS = "adapter_timeouts";
    public static final String METRIC_ADAPTER_BAD_RESPONSE = "adapter_bad_response";
    public static final String METRIC_ADAPTER_ERRORS = "adapter_errors";
    public static final String METRIC_REQUEST_LATENCY = "request_latency";
    public static final String METRIC_ENGINE_REJECTED_TOTAL = "engine_rejected_total";

    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_TYPE = "type";
    public static final String TAG_ADAPTER = "adapter";
    public static final String TAG_REASON = "reason";

    public static final String REASON_IN_FLIGHT_LIMIT = "in_flight_limit";

    private final MeterRegistry registry;

    public MetricsCollector(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequestOutcome(RequestOutcome outcome) {
        registry.counter(METRIC_REQUESTS_TOTAL, TAG_OUTCOME, outcome.value()).increment();
    }

    public void recordError(String type) {
        registry.counter(METRIC_ERRORS_TOTAL, TAG_TYPE, type).increment();
    }

    public void recordAdapterTimeout(String adapter) {
        registry.counter(METRIC_ADAPTER_TIMEOUTS, TAG_ADAPTER, adapter).increment();
    }

    public void recordAdapterBadResponse(String adapter) {
        registry.counter(METRIC_ADAPTER_BAD_RESPONSE, TAG_ADAPTER, adapter).increment();
    }

    public void recordAdapterError(String adapter) {
        registry.counter(METRIC_ADAPTER_ERRORS, TAG_ADAPTER, adapter).increment();
    }

    public void recordEngineRejected(String reason) {
        registry.counter(METRIC_ENGINE_REJECTED_TOTAL, TAG_REASON, reason).increment();
    }

    public void recordInFlightLimitRejection() {
        recordEngineRejected(REASON_IN_FLIGHT_LIMIT);
    }

    public Timer.Sample startRequestTimer() {
        return Timer.start(registry);
    }

    public void stopRequestTimer(Timer.Sample sample, RequestOutcome outcome) {
        sample.stop(Timer.builder(METRIC_REQUEST_LATENCY)
                .tag(TAG_OUTCOME, outcome.value())
                .publishPercentiles(0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(2))
                .register(registry));
    }
}
