package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

// Minimal impression model; unknown fields are ignored for 2.5/2.6 compatibility.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Imp(
        @NotBlank String id
) {
}
