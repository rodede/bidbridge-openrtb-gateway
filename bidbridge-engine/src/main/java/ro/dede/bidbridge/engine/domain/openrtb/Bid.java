package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// Minimal bid with required identifiers and positive price.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Bid(
        @NotBlank String id,
        @NotBlank String impid,
        @Positive double price,
        @NotBlank String adm
) {
}
