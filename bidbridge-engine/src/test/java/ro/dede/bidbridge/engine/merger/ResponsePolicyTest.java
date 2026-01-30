package ro.dede.bidbridge.engine.merger;

import org.junit.jupiter.api.Test;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedDevice;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedImp;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsePolicyTest {

    @Test
    void appliesDefaultCurrencyWhenMissing() {
        var properties = new ResponseProperties();
        properties.setDefaultCurrency("USD");
        var policy = new ResponsePolicy(properties);

        var normalized = policy.normalize(sampleRequest(), new SelectedBid("b1", "1", 1.2, "<a/>", null));

        assertTrue(normalized.isPresent());
        assertEquals("USD", normalized.get().currency());
    }

    @Test
    void rejectsBidWithUnknownImpid() {
        var policy = new ResponsePolicy(new ResponseProperties());

        var normalized = policy.normalize(sampleRequest(), new SelectedBid("b1", "missing", 1.2, "<a/>", "USD"));

        assertTrue(normalized.isEmpty());
    }

    @Test
    void rejectsDisallowedCurrency() {
        var properties = new ResponseProperties();
        properties.setAllowedCurrencies(List.of("EUR"));
        var policy = new ResponsePolicy(properties);

        var normalized = policy.normalize(sampleRequest(), new SelectedBid("b1", "1", 1.2, "<a/>", "USD"));

        assertTrue(normalized.isEmpty());
    }

    private NormalizedBidRequest sampleRequest() {
        return new NormalizedBidRequest(
                "req-1",
                List.of(new NormalizedImp("1", ImpType.BANNER, 1.0, Map.of())),
                InventoryType.SITE,
                100,
                new NormalizedDevice("ua", "ip", "os", 1, Map.of()),
                Map.of(),
                Map.of(),
                null,
                null,
                null
        );
    }
}
