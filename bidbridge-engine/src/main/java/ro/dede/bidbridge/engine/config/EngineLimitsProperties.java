package ro.dede.bidbridge.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * In-flight limit settings for engine bid endpoint.
 */
@ConfigurationProperties(prefix = "engine.limits")
public class EngineLimitsProperties {
    private int maxInFlight = 200;

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }
}
