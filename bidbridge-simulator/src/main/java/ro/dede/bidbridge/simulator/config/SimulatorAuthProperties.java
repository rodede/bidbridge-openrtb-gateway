package ro.dede.bidbridge.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Shared-secret auth settings for the simulator.
 */
@Configuration
@ConfigurationProperties(prefix = "simulator.auth")
public class SimulatorAuthProperties {
    private boolean enabled = false;
    private String bidApiKey;
    private String adminApiToken;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBidApiKey() {
        return bidApiKey;
    }

    public void setBidApiKey(String bidApiKey) {
        this.bidApiKey = bidApiKey;
    }

    public String getAdminApiToken() {
        return adminApiToken;
    }

    public void setAdminApiToken(String adminApiToken) {
        this.adminApiToken = adminApiToken;
    }
}
