package ro.dede.bidbridge.engine.merger;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.openrtb.Bid;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.domain.openrtb.SeatBid;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.service.AdapterFailureException;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.Comparator;
import java.util.List;

/**
 * Selects the highest-priced valid bid and builds the OpenRTB response.
 */
@Component
public class DefaultResponseMerger implements ResponseMerger {

    @Override
    public Mono<BidResponse> merge(NormalizedBidRequest request, List<AdapterResult> results) {
        var anySuccess = results.stream().anyMatch(result ->
                result.status() == AdapterResultStatus.BID || result.status() == AdapterResultStatus.NO_BID);
        if (!anySuccess) {
            var anyTimeout = results.stream().anyMatch(result -> result.status() == AdapterResultStatus.TIMEOUT);
            if (anyTimeout) {
                return Mono.error(new OverloadException("All adapters timed out"));
            }
            return Mono.error(new AdapterFailureException("All adapters failed"));
        }

        var best = results.stream()
                .filter(result -> result.status() == AdapterResultStatus.BID && result.bid() != null)
                .filter(result -> result.bid().price() > 0)
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
}
