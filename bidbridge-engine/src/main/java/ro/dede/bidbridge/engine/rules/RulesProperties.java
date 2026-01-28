package ro.dede.bidbridge.engine.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;

import java.util.ArrayList;
import java.util.List;

/**
 * Config-only rules for MVP filtering and routing.
 */
@ConfigurationProperties(prefix = "rules")
public class RulesProperties {

    // Allow/deny lists are optional; empty means no restriction.
    private List<InventoryType> allowInventory = new ArrayList<>();
    private List<InventoryType> denyInventory = new ArrayList<>();
    private Double minBidfloor;
    private List<String> allowAdapters = new ArrayList<>();
    private List<String> denyAdapters = new ArrayList<>();

    public List<InventoryType> getAllowInventory() {
        return allowInventory;
    }

    public void setAllowInventory(List<InventoryType> allowInventory) {
        this.allowInventory = allowInventory;
    }

    public List<InventoryType> getDenyInventory() {
        return denyInventory;
    }

    public void setDenyInventory(List<InventoryType> denyInventory) {
        this.denyInventory = denyInventory;
    }

    public Double getMinBidfloor() {
        return minBidfloor;
    }

    public void setMinBidfloor(Double minBidfloor) {
        this.minBidfloor = minBidfloor;
    }

    public List<String> getAllowAdapters() {
        return allowAdapters;
    }

    public void setAllowAdapters(List<String> allowAdapters) {
        this.allowAdapters = allowAdapters;
    }

    public List<String> getDenyAdapters() {
        return denyAdapters;
    }

    public void setDenyAdapters(List<String> denyAdapters) {
        this.denyAdapters = denyAdapters;
    }
}
