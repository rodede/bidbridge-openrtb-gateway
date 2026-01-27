package ro.dede.bidbridge.engine.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "adapters")
public class AdapterProperties {
    // Per-adapter configuration keyed by adapter name.
    private final Map<String, AdapterConfig> configs = new HashMap<>();

    public Map<String, AdapterConfig> getConfigs() {
        return configs;
    }

    public static class AdapterConfig {
        private boolean enabled = false;
        private String endpoint;
        private Integer timeoutMs;
        private Double bidProbability;
        private Double fixedPrice;
        private String admTemplate;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Double getBidProbability() {
            return bidProbability;
        }

        public void setBidProbability(Double bidProbability) {
            this.bidProbability = bidProbability;
        }

        public Double getFixedPrice() {
            return fixedPrice;
        }

        public void setFixedPrice(Double fixedPrice) {
            this.fixedPrice = fixedPrice;
        }

        public String getAdmTemplate() {
            return admTemplate;
        }

        public void setAdmTemplate(String admTemplate) {
            this.admTemplate = admTemplate;
        }
    }
}
