package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

// Marker type for banner impressions; fields not modeled are ignored.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Banner(
        Map<String, Object> ext
) implements HasExt {
}
