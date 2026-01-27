package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

// Minimal OpenRTB response with required id and optional seat bids.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidResponse(@NotBlank String id, List<@Valid SeatBid> seatbid, String cur) {
}
