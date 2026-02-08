package ro.dede.bidbridge.engine.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.adapters.AdapterContext;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry.AdapterEntry;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.merger.ResponseMerger;
import ro.dede.bidbridge.engine.observability.MetricsCollector;
import ro.dede.bidbridge.engine.rules.RulesEvaluator;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Executes enabled adapters within budget, selects the best bid, and builds the response.
 */
@Service
public class DefaultBidService implements BidService {
    private static final int MERGE_RESERVE_MS = 10;
    private final AdapterRegistry adapterRegistry;
    private final RulesEvaluator rulesEvaluator;
    private final ResponseMerger responseMerger;
    private final MetricsCollector metrics;
    private final BidServiceProperties properties;

    public DefaultBidService(AdapterRegistry adapterRegistry,
                             RulesEvaluator rulesEvaluator,
                             ResponseMerger responseMerger,
                             MetricsCollector metrics,
                             BidServiceProperties properties) {
        this.adapterRegistry = adapterRegistry;
        this.rulesEvaluator = rulesEvaluator;
        this.responseMerger = responseMerger;
        this.metrics = metrics;
        this.properties = properties;
    }

    /**
     * Runs all enabled adapters in parallel within the time budget.
     */
    @Override
    public Mono<BidResponse> bid(NormalizedBidRequest request) {
        var adapters = adapterRegistry.activeAdapters();
        if (adapters.isEmpty()) {
            return Mono.error(new ConfigurationException("No adapters enabled"));
        }

        var requestDeadlineMs = resolveDeadlineMs(request);
        if (requestDeadlineMs <= 0) {
            return Mono.error(new OverloadException("Request timed out"));
        }

        // Keep some budget for merge/response building.
        var adapterBudgetMs = Math.max(0, requestDeadlineMs - MERGE_RESERVE_MS);
        var rulesResult = rulesEvaluator.apply(request, adapters);

        return Flux.fromIterable(rulesResult.adapters())
                .flatMap(entry -> executeAdapter(entry, rulesResult.request(), adapterBudgetMs))
                .collectList()
                .flatMap(results -> responseMerger.merge(rulesResult.request(), results))
                .timeout(Duration.ofMillis(requestDeadlineMs))
                .onErrorMap(TimeoutException.class, ex -> new OverloadException("Request timed out"));
    }

    /**
     * Executes a single adapter with per-adapter timeout and error mapping.
     */
    private Mono<AdapterResult> executeAdapter(AdapterEntry entry,
                                               NormalizedBidRequest request,
                                               int budgetMs) {
        var configTimeout = entry.config().getTimeoutMs();
        var timeoutMs = configTimeout == null ? budgetMs : Math.min(configTimeout, budgetMs);
        if (timeoutMs <= 0) {
            return Mono.just(AdapterResult.timeout(entry.name()));
        }
        // Measure adapter latency and map timeouts/errors into adapter-level results.
        var context = new AdapterContext(entry.name(), entry.config());
        var start = System.nanoTime();
        return entry.adapter()
                .bid(request, context)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(BadBidderResponseException.class, ex -> {
                    metrics.recordAdapterBadResponse(entry.name());
                    return Mono.just(AdapterResult.error(entry.name(), "bad_bidder_response", messageOrDefault(ex, "Bad bidder response")));
                })
                .onErrorResume(TimeoutException.class, ex -> {
                    metrics.recordAdapterTimeout(entry.name());
                    return Mono.just(AdapterResult.timeout(entry.name()));
                })
                .onErrorResume(ex -> {
                    metrics.recordAdapterError(entry.name());
                    return Mono.just(AdapterResult.error(entry.name(), "adapter_error", messageOrDefault(ex, "Adapter error")));
                })
                .map(result -> result.withLatencyMs(toMillis(start)));
    }

    /**
     * Returns elapsed time in milliseconds.
     */
    private long toMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String messageOrDefault(Throwable ex, String fallback) {
        var message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private int resolveDeadlineMs(NormalizedBidRequest request) {
        var requestTmax = request.tmaxMs();
        var configured = properties.getGlobalTimeoutMs();
        if (configured == null || configured <= 0) {
            return requestTmax;
        }
        return Math.min(requestTmax, configured);
    }
}
