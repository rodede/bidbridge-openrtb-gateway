package ro.dede.bidbridge.engine.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;

@Service
public class DefaultBidService implements BidService {

    @Override
    public Mono<BidResponse> bid(NormalizedBidRequest request) {
        // Stub: no adapters wired yet, so default to no-bid.
        return Mono.empty();
    }
}
