package ro.dede.bidbridge.engine.domain.normalized;

import java.util.Map;

// Normalized impression with derived type and defaults applied.
public record NormalizedImp(
        String id,
        ImpType type,
        double bidfloor,
        Map<String, Object> ext
) {
}
