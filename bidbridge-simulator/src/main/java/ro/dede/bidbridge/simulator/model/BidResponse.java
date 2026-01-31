package ro.dede.bidbridge.simulator.model;

import java.util.List;

public record BidResponse(
        String id,
        List<SeatBid> seatbid,
        String cur
) {
}
