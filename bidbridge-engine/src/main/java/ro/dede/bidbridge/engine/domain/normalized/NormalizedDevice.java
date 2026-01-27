package ro.dede.bidbridge.engine.domain.normalized;

import java.util.Map;

// Selected device fields for routing and targeting, plus ext passthrough.
public record NormalizedDevice(
        String ua,
        String ip,
        String os,
        Integer devicetype,
        Map<String, Object> ext
) {
}
