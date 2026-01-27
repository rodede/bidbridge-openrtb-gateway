package ro.dede.bidbridge.engine.adapters;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registry that exposes enabled bidder adapters based on configuration.
 */
@Component
public class AdapterRegistry {
    private final Map<String, BidderAdapter> adapters;
    private final AdapterProperties properties;

    public AdapterRegistry(Map<String, BidderAdapter> adapters, AdapterProperties properties) {
        this.adapters = adapters;
        this.properties = properties;
    }

    // Returns only adapters explicitly enabled via configuration.
    public List<AdapterEntry> activeAdapters() {
        var active = new ArrayList<AdapterEntry>();
        for (var entry : adapters.entrySet()) {
            var name = entry.getKey();
            var config = properties.getConfigs().get(name);
            if (config == null || !config.isEnabled()) {
                continue;
            }
            active.add(new AdapterEntry(name, entry.getValue(), config));
        }
        return active;
    }

    public record AdapterEntry(String name, BidderAdapter adapter, AdapterProperties.AdapterConfig config) {
    }
}
