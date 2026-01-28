package ro.dede.bidbridge.engine.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry.AdapterEntry;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.service.FilteredRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Applies MVP rules (inventory, bidfloor, adapter allow/deny) in order.
 */
@Component
public class DefaultRulesEvaluator implements RulesEvaluator {
    private static final Logger log = LoggerFactory.getLogger(DefaultRulesEvaluator.class);
    private final RulesProperties properties;

    public DefaultRulesEvaluator(RulesProperties properties) {
        this.properties = properties;
    }

    @Override
    public RulesResult apply(NormalizedBidRequest request, List<AdapterEntry> adapters) {
        var applied = new ArrayList<String>();
        try {
            var filteredRequest = applyBidfloorRule(request, applied);
            var filteredAdapters = applyInventoryRules(filteredRequest, adapters, applied);
            filteredAdapters = applyAdapterRules(filteredAdapters, applied);

            if (!applied.isEmpty()) {
                var adapterNames = filteredAdapters.stream().map(AdapterEntry::name).toList();
                log.info("Rules applied for requestId={} rules={} adapters={}",
                        request.requestId(), applied, adapterNames);
            }
            if (filteredRequest.imps().isEmpty()) {
                throw new FilteredRequestException("All imps filtered by rules");
            }
            if (filteredAdapters.isEmpty()) {
                throw new FilteredRequestException("No adapters after rules");
            }

            return new RulesResult(filteredRequest, filteredAdapters, applied);
        } catch (FilteredRequestException ex) {
            if (!applied.isEmpty()) {
                log.info("Rules applied for requestId={} rules={} adapters=[]",
                        request.requestId(), applied);
            }
            throw ex;
        }
    }

    private NormalizedBidRequest applyBidfloorRule(NormalizedBidRequest request, List<String> applied) {
        var minBidfloor = properties.getMinBidfloor();
        if (minBidfloor == null) {
            return request;
        }
        var filteredImps = request.imps().stream()
                .filter(imp -> imp.bidfloor() >= minBidfloor)
                .toList();
        if (filteredImps.size() != request.imps().size()) {
            applied.add("minBidfloor");
        }
        return new NormalizedBidRequest(
                request.requestId(),
                filteredImps,
                request.inventoryType(),
                request.tmaxMs(),
                request.device(),
                request.ext(),
                request.siteExt(),
                request.appExt(),
                request.userExt(),
                request.regsExt()
        );
    }

    private List<AdapterEntry> applyInventoryRules(NormalizedBidRequest request,
                                                   List<AdapterEntry> adapters,
                                                   List<String> applied) {
        var allow = properties.getAllowInventory();
        if (allow != null && !allow.isEmpty() && !allow.contains(request.inventoryType())) {
            applied.add("allowInventory");
            throw new FilteredRequestException("Inventory not allowed");
        }
        var deny = properties.getDenyInventory();
        if (deny != null && deny.contains(request.inventoryType())) {
            applied.add("denyInventory");
            throw new FilteredRequestException("Inventory denied");
        }
        return adapters;
    }

    private List<AdapterEntry> applyAdapterRules(List<AdapterEntry> adapters, List<String> applied) {
        var allow = properties.getAllowAdapters();
        var deny = properties.getDenyAdapters();

        var filtered = adapters;
        if (allow != null && !allow.isEmpty()) {
            filtered = filtered.stream()
                    .filter(entry -> allow.contains(entry.name()))
                    .collect(Collectors.toList());
            applied.add("allowAdapters");
        }
        if (deny != null && !deny.isEmpty()) {
            filtered = filtered.stream()
                    .filter(entry -> !deny.contains(entry.name()))
                    .collect(Collectors.toList());
            applied.add("denyAdapters");
        }
        return filtered;
    }
}
