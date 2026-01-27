package ro.dede.bidbridge.engine.domain.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

// Minimal device fields used for targeting/routing decisions.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Device(
        String ua,
        String ip,
        String os,
        Integer devicetype,
        Map<String, Object> ext
) implements HasExt {
}
