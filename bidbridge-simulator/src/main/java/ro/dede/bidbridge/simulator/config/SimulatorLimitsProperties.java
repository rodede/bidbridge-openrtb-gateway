package ro.dede.bidbridge.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Simulator runtime limits for request handling.
 */
@Configuration
@ConfigurationProperties(prefix = "simulator")
public class SimulatorLimitsProperties {
    private int maxInFlight = 200;

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }
}
