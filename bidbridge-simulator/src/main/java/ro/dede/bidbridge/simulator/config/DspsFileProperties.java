package ro.dede.bidbridge.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Points to the external dsps.yml file used by the simulator.
 */
@Configuration
@ConfigurationProperties(prefix = "dsps")
public class DspsFileProperties {
    private String file = "dsps.yml";
    private int pollIntervalMs = 2000;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }
}
