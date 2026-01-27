package ro.dede.bidbridge.engine.adapters;

import org.junit.jupiter.api.Test;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedDevice;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedImp;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimulatorAdapterTest {

    private final SimulatorAdapter adapter = new SimulatorAdapter();

    @Test
    void returnsBidWhenProbabilityIsOne() {
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        config.setBidProbability(1.0);
        config.setFixedPrice(2.5);
        config.setAdmTemplate("<adm/>");
        var context = new AdapterContext("simulator", config);

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.BID, result.status());
        assertEquals(2.5, result.bid().price());
        assertEquals("<adm/>", result.bid().adm());
    }

    @Test
    void returnsNoBidWhenProbabilityIsZero() {
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        config.setBidProbability(0.0);
        var context = new AdapterContext("simulator", config);

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.NO_BID, result.status());
    }

    private NormalizedBidRequest sampleRequest() {
        return new NormalizedBidRequest(
                "req-1",
                List.of(new NormalizedImp("1", ImpType.BANNER, 0.0, Map.of())),
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
