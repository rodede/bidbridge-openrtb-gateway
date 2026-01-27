package ro.dede.bidbridge.engine.normalization;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;

public interface BidRequestNormalizer {
    Mono<NormalizedBidRequest> normalize(BidRequest request);
}
