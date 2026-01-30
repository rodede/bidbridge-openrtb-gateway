package ro.dede.bidbridge.engine.merger;

import org.springframework.stereotype.Component;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

@Component
public class ResponsePolicy {
    private final ResponseProperties properties;

    public ResponsePolicy(ResponseProperties properties) {
        this.properties = properties;
    }

    public Optional<SelectedBid> normalize(NormalizedBidRequest request, SelectedBid bid) {
        if (bid == null) {
            return Optional.empty();
        }
        if (isBlank(bid.id()) || isBlank(bid.impid())) {
            return Optional.empty();
        }
        if (bid.price() <= 0) {
            return Optional.empty();
        }
        var imps = request.imps();
        if (imps == null || imps.isEmpty()) {
            return Optional.empty();
        }
        var impIds = new HashSet<String>(imps.size());
        for (var imp : imps) {
            if (imp != null && !isBlank(imp.id())) {
                impIds.add(imp.id());
            }
        }
        if (!impIds.contains(bid.impid())) {
            return Optional.empty();
        }

        var normalizedCurrency = normalizeCurrency(bid.currency());
        if (!isAllowedCurrency(normalizedCurrency)) {
            return Optional.empty();
        }

        return Optional.of(new SelectedBid(
                bid.id(),
                bid.impid(),
                bid.price(),
                bid.adm(),
                normalizedCurrency
        ));
    }

    private String normalizeCurrency(String currency) {
        if (isBlank(currency)) {
            return defaultCurrency();
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private boolean isAllowedCurrency(String currency) {
        var allowed = properties.getAllowedCurrencies();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        for (var allowedCurrency : allowed) {
            if (currency.equalsIgnoreCase(allowedCurrency)) {
                return true;
            }
        }
        return false;
    }

    private String defaultCurrency() {
        var configured = properties.getDefaultCurrency();
        return isBlank(configured) ? "USD" : configured.toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
