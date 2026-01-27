package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

// Minimal OpenRTB request with validation on required fields.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidRequest(
        @NotBlank String id,
        @NotEmpty List<@Valid Imp> imp,
        Integer tmax,
        Map<String, Object> ext
) {
}
