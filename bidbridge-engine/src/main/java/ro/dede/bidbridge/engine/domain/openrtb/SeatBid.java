package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Minimal seat bid wrapper for bid list.
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeatBid(
        @NotEmpty List<@Valid Bid> bid
) {
}
