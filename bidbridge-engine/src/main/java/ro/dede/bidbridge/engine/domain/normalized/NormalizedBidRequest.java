package ro.dede.bidbridge.engine.domain.normalized;

import java.util.List;
import java.util.Map;

// Normalized request with derived context and defaults applied.
public record NormalizedBidRequest(
        String requestId,
        List<NormalizedImp> imps,
        InventoryType inventoryType,
        int tmaxMs,
        NormalizedDevice device,
        Map<String, Object> ext,
        Map<String, Object> siteExt,
        Map<String, Object> appExt,
        Map<String, Object> userExt,
        Map<String, Object> regsExt
) {
}
