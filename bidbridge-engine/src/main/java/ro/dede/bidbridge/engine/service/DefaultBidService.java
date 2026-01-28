package ro.dede.bidbridge.engine.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.adapters.AdapterContext;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry.AdapterEntry;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.Bid;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.domain.openrtb.SeatBid;
import ro.dede.bidbridge.engine.rules.RulesEvaluator;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Executes enabled adapters within budget, selects the best bid, and builds the response.
 */
@Service
public class DefaultBidService implements BidService {
    private static final int MERGE_RESERVE_MS = 10;
    private final AdapterRegistry adapterRegistry;
    private final RulesEvaluator rulesEvaluator;

    public DefaultBidService(AdapterRegistry adapterRegistry, RulesEvaluator rulesEvaluator) {
        this.adapterRegistry = adapterRegistry;
        this.rulesEvaluator = rulesEvaluator;
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

        // Keep some budget for merge/response building.
        var adapterBudgetMs = Math.max(0, request.tmaxMs() - MERGE_RESERVE_MS);
        var rulesResult = rulesEvaluator.apply(request, adapters);

        return Flux.fromIterable(rulesResult.adapters())
                .flatMap(entry -> executeAdapter(entry, rulesResult.request(), adapterBudgetMs))
                .collectList()
                .flatMap(results -> mergeResults(rulesResult.request(), results));
    }

    /**
     * Executes a single adapter with per-adapter timeout and error mapping.
     */
    private Mono<AdapterResult> executeAdapter(AdapterEntry entry, NormalizedBidRequest request, int budgetMs) {
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
                .onErrorResume(TimeoutException.class, ex -> Mono.just(AdapterResult.timeout(entry.name())))
                .onErrorResume(ex -> Mono.just(AdapterResult.error(entry.name(), "adapter_error", ex.getMessage())))
                .map(result -> result.withLatencyMs(toMillis(start)));
    }

    /**
     * Selects the highest-priced bid; returns no-bid if no adapter bids.
     */
    private Mono<BidResponse> mergeResults(NormalizedBidRequest request, List<AdapterResult> results) {
        var anySuccess = results.stream().anyMatch(result ->
                result.status() == AdapterResultStatus.BID ||
                        result.status() == AdapterResultStatus.NO_BID);
        if (!anySuccess) {
            var anyTimeout = results.stream().anyMatch(result -> result.status() == AdapterResultStatus.TIMEOUT);
            if (anyTimeout) {
                return Mono.error(new OverloadException("All adapters timed out"));
            }
            return Mono.error(new AdapterFailureException("All adapters failed"));
        }

        var best = results.stream()
                .filter(result -> result.status() == AdapterResultStatus.BID && result.bid() != null)
                .max(Comparator.comparingDouble(result -> result.bid().price()));

        if (best.isEmpty()) {
            return Mono.empty();
        }

        var bid = best.get().bid();
        var responseBid = new Bid(bid.id(), bid.impid(), bid.price(), bid.adm());
        var response = new BidResponse(
                request.requestId(),
                List.of(new SeatBid(List.of(responseBid))),
                bid.currency()
        );
        return Mono.just(response);
    }

    /**
     * Returns elapsed time in milliseconds.
     */
    private long toMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
