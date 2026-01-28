package ro.dede.bidbridge.engine.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.adapters.AdapterProperties;
import ro.dede.bidbridge.engine.adapters.AdapterRegistry;
import ro.dede.bidbridge.engine.adapters.BidderAdapter;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.rules.DefaultRulesEvaluator;
import ro.dede.bidbridge.engine.rules.RulesProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBidServiceTest {

    @Test
    void returnsBidFromBestAdapter() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());
        properties.getConfigs().put("b", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.just(AdapterResult.bid("a",
                new SelectedBid("bid-a", "1", 1.0, "<a/>", "USD"), null));
        BidderAdapter adapterB = (request, context) -> Mono.just(AdapterResult.bid("b",
                new SelectedBid("bid-b", "1", 2.0, "<b/>", "USD"), null));

        var registry = new AdapterRegistry(Map.of("a", adapterA, "b", adapterB), properties);
        var service = new DefaultBidService(registry, rulesEvaluator());

        var response = service.bid(sampleRequest()).block();

        assertNotNull(response);
        assertEquals("req-1", response.id());
        assertEquals(2.0, response.seatbid().getFirst().bid().getFirst().price());
    }

    @Test
    void returnsNoBidWhenAtLeastOneAdapterCompletes() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());
        properties.getConfigs().put("b", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.just(AdapterResult.noBid("a", null));
        BidderAdapter adapterB = (request, context) -> Mono.error(new TimeoutException("timeout"));

        var registry = new AdapterRegistry(Map.of("a", adapterA, "b", adapterB), properties);
        var service = new DefaultBidService(registry, rulesEvaluator());

        var result = service.bid(sampleRequest()).blockOptional();

        assertEquals(true, result.isEmpty());
    }

    @Test
    void returnsOverloadWhenAllAdaptersFail() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());
        properties.getConfigs().put("b", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.error(new TimeoutException("timeout"));
        BidderAdapter adapterB = (request, context) -> Mono.error(new TimeoutException("timeout"));

        var registry = new AdapterRegistry(Map.of("a", adapterA, "b", adapterB), properties);
        var service = new DefaultBidService(registry, rulesEvaluator());

        assertThrows(OverloadException.class, () -> service.bid(sampleRequest()).block());
    }

    @Test
    void returnsAdapterFailureWhenAllAdaptersError() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());
        properties.getConfigs().put("b", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.error(new RuntimeException("boom"));
        BidderAdapter adapterB = (request, context) -> Mono.error(new RuntimeException("boom"));

        var registry = new AdapterRegistry(Map.of("a", adapterA, "b", adapterB), properties);
        var service = new DefaultBidService(registry, rulesEvaluator());

        assertThrows(AdapterFailureException.class, () -> service.bid(sampleRequest()).block());
    }

    @Test
    void throwsConfigurationWhenNoAdaptersEnabled() {
        var properties = new AdapterProperties();
        var registry = new AdapterRegistry(Map.of(), properties);
        var service = new DefaultBidService(registry, rulesEvaluator());

        assertThrows(ConfigurationException.class, () -> service.bid(sampleRequest()).block());
    }

    @Test
    void returnsNoBidWhenRulesFilterAllAdapters() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.just(AdapterResult.bid("a",
                new SelectedBid("bid-a", "1", 1.0, "<a/>", "USD"), null));

        var registry = new AdapterRegistry(Map.of("a", adapterA), properties);
        var rules = new RulesProperties();
        rules.setDenyAdapters(List.of("a"));
        var service = new DefaultBidService(registry, new DefaultRulesEvaluator(rules));

        assertThrows(FilteredRequestException.class, () -> service.bid(sampleRequest()).block());
    }

    @Test
    void returnsNoBidWhenRulesFilterAllImps() {
        var properties = new AdapterProperties();
        properties.getConfigs().put("a", enabledConfig());

        BidderAdapter adapterA = (request, context) -> Mono.just(AdapterResult.bid("a",
                new SelectedBid("bid-a", "1", 1.0, "<a/>", "USD"), null));

        var registry = new AdapterRegistry(Map.of("a", adapterA), properties);
        var rules = new RulesProperties();
        rules.setMinBidfloor(10.0);
        var service = new DefaultBidService(registry, new DefaultRulesEvaluator(rules));

        assertThrows(FilteredRequestException.class, () -> service.bid(sampleRequest()).block());
    }

    private AdapterProperties.AdapterConfig enabledConfig() {
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        config.setTimeoutMs(50);
        return config;
    }

    private NormalizedBidRequest sampleRequest() {
        return new NormalizedBidRequest(
                "req-1",
                List.of(new NormalizedImp("1", ImpType.BANNER, 0.0, Map.of())),
                InventoryType.SITE,
                100,
                new NormalizedDevice("ua", "ip", "os", 1, Map.of()),
                Map.of(),
                Map.of(),
                null,
                null,
                null
        );
    }

    private DefaultRulesEvaluator rulesEvaluator() {
        return new DefaultRulesEvaluator(new RulesProperties());
    }
}
