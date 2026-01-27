package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Imp(
        @NotBlank String id,
        Banner banner,
        Video video,
        Audio audio,
        @JsonProperty("native") Native nativeObject,
        Double bidfloor,
        Map<String, Object> ext
) implements HasExt {
}
