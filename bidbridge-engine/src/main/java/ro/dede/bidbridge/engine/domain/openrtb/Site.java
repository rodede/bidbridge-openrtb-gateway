package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

// Minimal site context; ext preserves partner-specific data.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Site(
        Map<String, Object> ext
) implements HasExt {
}
