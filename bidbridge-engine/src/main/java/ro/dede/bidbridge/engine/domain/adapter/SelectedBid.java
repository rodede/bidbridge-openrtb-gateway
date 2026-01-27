package ro.dede.bidbridge.engine.domain.adapter;

// Minimal internal bid used for selection/merging.
public record SelectedBid(
        String id,
        String impid,
        double price,
        String adm,
        String currency
) {
}
