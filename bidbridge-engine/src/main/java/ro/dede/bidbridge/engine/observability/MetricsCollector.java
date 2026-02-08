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
    private final MeterRegistry registry;

    public MetricsCollector(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequestOutcome(String outcome) {
        registry.counter("requests_total", "outcome", outcome).increment();
    }

    public void recordError(String type) {
        registry.counter("errors_total", "type", type).increment();
    }

    public void recordAdapterTimeout(String adapter) {
        registry.counter("adapter_timeouts", "adapter", adapter).increment();
    }

    public void recordAdapterBadResponse(String adapter) {
        registry.counter("adapter_bad_response", "adapter", adapter).increment();
    }

    public void recordAdapterError(String adapter) {
        registry.counter("adapter_errors", "adapter", adapter).increment();
    }

    public Timer.Sample startRequestTimer() {
        return Timer.start(registry);
    }

    public void stopRequestTimer(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("request_latency")
                .tag("outcome", outcome)
                .publishPercentiles(0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(2))
                .register(registry));
    }
}
