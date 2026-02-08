package ro.dede.bidbridge.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared-secret auth settings for engine bid endpoint.
 */
@ConfigurationProperties(prefix = "engine.auth")
public class EngineAuthProperties {
    private boolean enabled = false;
    private String bidApiKey;

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
}
