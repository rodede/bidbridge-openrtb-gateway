package ro.dede.bidbridge.engine.adapters.http;

import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.adapters.AdapterContext;
import ro.dede.bidbridge.engine.adapters.BidderAdapter;
import ro.dede.bidbridge.engine.domain.adapter.AdapterDebug;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

/**
 * Base class for HTTP bidder adapters using HttpBidderClient.
 */
public abstract class AbstractHttpAdapter<Req, Resp> implements BidderAdapter {
    private final HttpBidderClient<Resp> client;

    protected AbstractHttpAdapter(HttpBidderClient<Resp> client) {
        this.client = client;
    }

    @Override
    public final Mono<AdapterResult> bid(NormalizedBidRequest request, AdapterContext context) {
        var endpoint = context.config().getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return Mono.just(error(context, null, null, "missing_endpoint", "Missing adapter endpoint"));
        }
        var httpRequest = buildRequest(request);
        return client.postJson(endpoint, httpRequest)
                .flatMap(response -> mapResponse(request, context, response));
    }

    protected abstract Req buildRequest(NormalizedBidRequest request);

    protected abstract SelectedBid extractBid(NormalizedBidRequest request, Resp response);

    private Mono<AdapterResult> mapResponse(NormalizedBidRequest request,
                                            AdapterContext context,
                                            HttpBidderResponse<Resp> response) {
        var debug = new AdapterDebug(response.status(), response.responseSize(), null, null);
        if (response.status() == 204) {
            return Mono.just(AdapterResult.noBid(context.bidder(), debug));
        }
        if (response.status() < 200 || response.status() >= 300) {
            return Mono.just(error(context, response.status(), response.responseSize(),
                    "http_status", "HTTP " + response.status()));
        }
        if (response.body() == null) {
            return Mono.just(AdapterResult.noBid(context.bidder(), debug));
        }
        var selected = extractBid(request, response.body());
        if (selected == null || selected.price() <= 0) {
            return Mono.just(AdapterResult.noBid(context.bidder(), debug));
        }
        return Mono.just(new AdapterResult(context.bidder(), AdapterResultStatus.BID, null, selected, debug));
    }

    protected AdapterResult error(AdapterContext context,
                                  Integer status,
                                  Integer responseSize,
                                  String code,
                                  String message) {
        return new AdapterResult(context.bidder(), AdapterResultStatus.ERROR, null, null,
                new AdapterDebug(status, responseSize, code, message));
    }
}
