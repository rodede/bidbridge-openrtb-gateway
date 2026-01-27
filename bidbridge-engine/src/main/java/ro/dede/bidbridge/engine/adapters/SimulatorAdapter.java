package ro.dede.bidbridge.engine.adapters;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

import java.util.concurrent.ThreadLocalRandom;

// Configurable mock adapter for MVP demos and tests.
@Component("simulator")
public class SimulatorAdapter implements BidderAdapter {

    private static final double DEFAULT_PROBABILITY = 1.0;
    private static final double DEFAULT_PRICE = 1.0;
    private static final String DEFAULT_ADM = "<vast/>";

    @Override
    public Mono<AdapterResult> bid(NormalizedBidRequest request, AdapterContext context) {
        // Pure CPU work; defer computation to avoid blocking.
        return Mono.fromSupplier(() -> simulateBid(request, context));
    }

    private AdapterResult simulateBid(NormalizedBidRequest request, AdapterContext context) {
        var config = context.config();
        var probability = config.getBidProbability() == null ? DEFAULT_PROBABILITY : config.getBidProbability();
        if (ThreadLocalRandom.current().nextDouble() > probability) {
            return AdapterResult.noBid(context.bidder(), null);
        }
        var price = config.getFixedPrice() == null ? DEFAULT_PRICE : config.getFixedPrice();
        var adm = config.getAdmTemplate() == null ? DEFAULT_ADM : config.getAdmTemplate();
        var impid = request.imps().getFirst().id();
        var bid = new SelectedBid("sim-" + impid, impid, price, adm, null);
        return AdapterResult.bid(context.bidder(), bid, null);
    }
}
