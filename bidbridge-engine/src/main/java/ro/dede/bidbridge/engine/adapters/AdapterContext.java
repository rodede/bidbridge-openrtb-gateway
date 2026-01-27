package ro.dede.bidbridge.engine.adapters;

// Adapter-specific config and identity passed to adapter invocations.
public record AdapterContext(
        String bidder,
        AdapterProperties.AdapterConfig config
) {
}
