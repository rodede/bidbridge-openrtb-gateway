package ro.dede.bidbridge.simulator;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Default DSP bidder implementation producing fixed-price responses.
 */
@Component
public class DefaultDspBidder implements DspBidder {

    @Override
    public Mono<BidResponse> bid(BidRequest request, DspConfig config) {
        var impId = request.imp().getFirst().id();
        var bidId = "bid-" + request.id();
        var price = config.getFixedPrice() > 0 ? config.getFixedPrice() : 0.01;
        var adm = normalize(config.getAdmTemplate(), "<vast/>");
        var currency = normalize(config.getCurrency(), "RON");
        var bid = new Bid(bidId, impId, price, adm);
        var response = new BidResponse(
                request.id(),
                List.of(new SeatBid(List.of(bid))),
                currency
        );
        return Mono.just(response);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
