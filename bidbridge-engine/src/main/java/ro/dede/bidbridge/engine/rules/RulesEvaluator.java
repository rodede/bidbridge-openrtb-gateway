package ro.dede.bidbridge.engine.rules;

import ro.dede.bidbridge.engine.adapters.AdapterRegistry.AdapterEntry;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

import java.util.List;

/**
 * Applies configured rules to a request and adapter set.
 */
public interface RulesEvaluator {
    /**
     * Returns a filtered request and adapter list plus applied rule names.
     */
    RulesResult apply(NormalizedBidRequest request, List<AdapterEntry> adapters);
}
