package ro.dede.bidbridge.engine.adapters.http;

/**
 * Lightweight HTTP response container for adapter parsing.
 */
public record HttpBidderResponse<T>(
        int status,
        T body,
        Integer responseSize
) {
}
