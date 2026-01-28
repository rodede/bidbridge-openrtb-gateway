package ro.dede.bidbridge.engine.rules;

import org.junit.jupiter.api.Test;
import ro.dede.bidbridge.engine.adapters.AdapterProperties;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry;
import ro.dede.bidbridge.engine.adapters.BidderAdapter;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.normalized.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRulesEvaluatorTest {

    @Test
    void allowAllByDefault() {
        var evaluator = new DefaultRulesEvaluator(new RulesProperties());
        var adapters = sampleAdapters("a", "b");

        var result = evaluator.apply(sampleRequest(InventoryType.SITE), adapters);

        assertEquals(2, result.adapters().size());
        assertTrue(result.appliedRules().isEmpty());
    }

    @Test
    void filtersImpsByMinBidfloor() {
        var props = new RulesProperties();
        props.setMinBidfloor(1.0);
        var evaluator = new DefaultRulesEvaluator(props);

        var request = new NormalizedBidRequest(
                "req-1",
                List.of(
                        new NormalizedImp("1", ImpType.BANNER, 0.5, Map.of()),
                        new NormalizedImp("2", ImpType.BANNER, 2.0, Map.of())
                ),
                InventoryType.SITE,
                100,
                new NormalizedDevice("ua", "ip", "os", 1, Map.of()),
                Map.of(),
                Map.of(),
                null,
                null,
                null
        );

        var result = evaluator.apply(request, sampleAdapters("a"));

        assertEquals(1, result.request().imps().size());
        assertTrue(result.appliedRules().contains("minBidfloor"));
    }

    @Test
    void blocksInventoryWhenNotAllowed() {
        var props = new RulesProperties();
        props.setAllowInventory(List.of(InventoryType.SITE));
        var evaluator = new DefaultRulesEvaluator(props);

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                ro.dede.bidbridge.engine.service.FilteredRequestException.class,
                () -> evaluator.apply(sampleRequest(InventoryType.APP), sampleAdapters("a"))
        );
        assertTrue(ex.getMessage().contains("Inventory not allowed"));
    }

    @Test
    void filtersAdaptersWithAllowAndDeny() {
        var props = new RulesProperties();
        props.setAllowAdapters(List.of("a", "b"));
        props.setDenyAdapters(List.of("b"));
        var evaluator = new DefaultRulesEvaluator(props);

        var result = evaluator.apply(sampleRequest(InventoryType.SITE), sampleAdapters("a", "b", "c"));

        assertEquals(1, result.adapters().size());
        assertEquals("a", result.adapters().getFirst().name());
        assertTrue(result.appliedRules().contains("allowAdapters"));
        assertTrue(result.appliedRules().contains("denyAdapters"));
    }

    private List<AdapterRegistry.AdapterEntry> sampleAdapters(String... names) {
        BidderAdapter adapter = (request, context) -> reactor.core.publisher.Mono.just(AdapterResult.noBid(context.bidder(), null));
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        return java.util.Arrays.stream(names)
                .map(name -> new AdapterRegistry.AdapterEntry(name, adapter, config))
                .toList();
    }

    private NormalizedBidRequest sampleRequest(InventoryType inventoryType) {
        return new NormalizedBidRequest(
                "req-1",
                List.of(new NormalizedImp("1", ImpType.BANNER, 1.0, Map.of())),
                inventoryType,
                100,
                new NormalizedDevice("ua", "ip", "os", 1, Map.of()),
                Map.of(),
                Map.of(),
                null,
                null,
                null
        );
    }
}
