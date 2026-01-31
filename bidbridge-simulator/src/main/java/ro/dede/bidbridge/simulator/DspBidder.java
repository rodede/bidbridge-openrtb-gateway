package ro.dede.bidbridge.simulator;

import reactor.core.publisher.Mono;

/**
 * Strategy for producing a bid response for a given DSP config.
 */
public interface DspBidder {
    Mono<BidResponse> bid(BidRequest request, DspConfig config);
}
