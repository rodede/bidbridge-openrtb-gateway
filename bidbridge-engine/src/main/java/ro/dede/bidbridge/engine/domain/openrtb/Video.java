package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

// Marker type for video impressions; fields not modeled are ignored.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Video(
        Map<String, Object> ext
) implements HasExt {
}
