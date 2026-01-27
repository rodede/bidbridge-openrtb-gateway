package ro.dede.bidbridge.engine.adapters;

import org.junit.jupiter.api.Test;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterRegistryTest {

    @Test
    void filtersEnabledAdapters() {
        var properties = new AdapterProperties();
        var enabled = new AdapterProperties.AdapterConfig();
        enabled.setEnabled(true);
        var disabled = new AdapterProperties.AdapterConfig();
        disabled.setEnabled(false);
        properties.getConfigs().put("simulator", enabled);
        properties.getConfigs().put("other", disabled);

        var registry = new AdapterRegistry(
                Map.of(
                        "simulator", (request, context) -> reactor.core.publisher.Mono.just(AdapterResult.noBid("simulator", null)),
                        "other", (request, context) -> reactor.core.publisher.Mono.just(AdapterResult.noBid("other", null))
                ),
                properties
        );

        var active = registry.activeAdapters();

        assertEquals(1, active.size());
        assertTrue(active.stream().anyMatch(entry -> entry.name().equals("simulator")));
    }
}
