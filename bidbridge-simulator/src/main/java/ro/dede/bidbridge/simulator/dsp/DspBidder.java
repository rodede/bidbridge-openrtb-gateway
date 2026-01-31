package ro.dede.bidbridge.simulator.dsp;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.config.DspConfig;
import ro.dede.bidbridge.simulator.model.BidRequest;
import ro.dede.bidbridge.simulator.model.BidResponse;

/**
 * Strategy for producing a bid response for a given DSP config.
 */
public interface DspBidder {
    Mono<BidResponse> bid(BidRequest request, DspConfig config);
}
