package ro.dede.bidbridge.simulator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Bid(
        String id,
        String impid,
        double price,
        @JsonProperty("adm") String adm
) {
}
