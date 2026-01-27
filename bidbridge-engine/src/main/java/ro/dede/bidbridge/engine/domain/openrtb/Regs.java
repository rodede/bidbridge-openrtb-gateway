package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

// Minimal regs object; ext carries privacy/partner-specific signals.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Regs(
        Map<String, Object> ext
) implements HasExt {
}
