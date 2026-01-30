package ro.dede.bidbridge.engine.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bid")
public class BidServiceProperties {
    private Integer globalTimeoutMs;

    public Integer getGlobalTimeoutMs() {
        return globalTimeoutMs;
    }

    public void setGlobalTimeoutMs(Integer globalTimeoutMs) {
        this.globalTimeoutMs = globalTimeoutMs;
    }
}
