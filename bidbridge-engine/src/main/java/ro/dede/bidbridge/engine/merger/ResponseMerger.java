package ro.dede.bidbridge.engine.merger;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;

import java.util.List;

/**
 * Merges adapter results into a single OpenRTB response.
 */
public interface ResponseMerger {
    Mono<BidResponse> merge(NormalizedBidRequest request, List<AdapterResult> results);
}
