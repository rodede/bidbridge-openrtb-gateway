package ro.dede.bidbridge.engine.merger;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "response")
public class ResponseProperties {
    private String defaultCurrency = "USD";
    private List<String> allowedCurrencies = new ArrayList<>();

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public List<String> getAllowedCurrencies() {
        return allowedCurrencies;
    }

    public void setAllowedCurrencies(List<String> allowedCurrencies) {
        this.allowedCurrencies = allowedCurrencies;
    }
}
