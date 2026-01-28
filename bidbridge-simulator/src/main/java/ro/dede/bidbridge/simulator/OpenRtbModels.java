package ro.dede.bidbridge.simulator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record BidRequest(
        String id,
        List<Imp> imp
) {
}

record Imp(
        String id
) {
}

record BidResponse(
        String id,
        List<SeatBid> seatbid,
        String cur
) {
}

record SeatBid(
        List<Bid> bid
) {
}

record Bid(
        String id,
        String impid,
        double price,
        @JsonProperty("adm") String adm
) {
}
