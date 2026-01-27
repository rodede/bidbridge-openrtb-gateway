package ro.dede.bidbridge.engine.domain.adapter;

// Adapter execution outcome with selected bid and lightweight debug info.
public record AdapterResult(
        String bidder,
        AdapterResultStatus status,
        Long latencyMs,
        SelectedBid bid,
        AdapterDebug debug
) {
    public AdapterResult withLatencyMs(long latencyMs) {
        return new AdapterResult(bidder, status, latencyMs, bid, debug);
    }

    public static AdapterResult bid(String bidder, SelectedBid bid, AdapterDebug debug) {
        return new AdapterResult(bidder, AdapterResultStatus.BID, null, bid, debug);
    }

    public static AdapterResult noBid(String bidder, AdapterDebug debug) {
        return new AdapterResult(bidder, AdapterResultStatus.NO_BID, null, null, debug);
    }

    public static AdapterResult timeout(String bidder) {
        return new AdapterResult(bidder, AdapterResultStatus.TIMEOUT, null, null, new AdapterDebug(null, null, "timeout", "Timeout"));
    }

    public static AdapterResult error(String bidder, String code, String message) {
        return new AdapterResult(bidder, AdapterResultStatus.ERROR, null, null, new AdapterDebug(null, null, code, message));
    }
}
