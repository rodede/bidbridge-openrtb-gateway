package ro.dede.bidbridge.simulator.model;

import java.util.List;

public record BidRequest(
        String id,
        List<Imp> imp
) {
}
