package ro.dede.bidbridge.engine.adapters;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

public interface BidderAdapter {
    Mono<AdapterResult> bid(NormalizedBidRequest request, AdapterContext context);
}
