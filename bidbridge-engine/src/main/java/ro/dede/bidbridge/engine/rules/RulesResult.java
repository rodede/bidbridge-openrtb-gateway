package ro.dede.bidbridge.engine.rules;

import ro.dede.bidbridge.engine.adapters.AdapterRegistry.AdapterEntry;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

import java.util.List;

// Result of rules evaluation with filtered request/adapters and applied rule names.
public record RulesResult(
        NormalizedBidRequest request,
        List<AdapterEntry> adapters,
        List<String> appliedRules
) {
}
