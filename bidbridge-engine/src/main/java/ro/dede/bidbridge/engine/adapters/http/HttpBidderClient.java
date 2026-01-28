package ro.dede.bidbridge.engine.adapters.http;

import reactor.core.publisher.Mono;

/**
 * HTTP client abstraction for bidder integrations.
 */
public interface HttpBidderClient<T> {
    Mono<HttpBidderResponse<T>> postJson(String endpoint, Object body);
}
