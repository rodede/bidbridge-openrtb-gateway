package ro.dede.bidbridge.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * DSP configuration registry for the simulator, keyed by dsp name.
 */
@Configuration
@ConfigurationProperties(prefix = "dsps")
public class DspProperties {
    private Map<String, DspConfig> configs = new HashMap<>();

    public Map<String, DspConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, DspConfig> configs) {
        this.configs = configs;
    }

    /**
     * Per-DSP response configuration used by the simulator.
     */
    public static class DspConfig {
        private boolean enabled = true;
        private double bidProbability = 1.0;
        private double fixedPrice = 1.5;
        private String currency = "USD";
        private String admTemplate = "<adm/>";
        private int responseDelayMs = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getBidProbability() {
            return bidProbability;
        }

        public void setBidProbability(double bidProbability) {
            this.bidProbability = bidProbability;
        }

        public double getFixedPrice() {
            return fixedPrice;
        }

        public void setFixedPrice(double fixedPrice) {
            this.fixedPrice = fixedPrice;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getAdmTemplate() {
            return admTemplate;
        }

        public void setAdmTemplate(String admTemplate) {
            this.admTemplate = admTemplate;
        }

        public int getResponseDelayMs() {
            return responseDelayMs;
        }

        public void setResponseDelayMs(int responseDelayMs) {
            this.responseDelayMs = responseDelayMs;
        }
    }
}
