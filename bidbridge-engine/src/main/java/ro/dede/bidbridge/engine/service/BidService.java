package ro.dede.bidbridge.engine.service;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;

public interface BidService {
    Mono<BidResponse> bid(NormalizedBidRequest request);
}
