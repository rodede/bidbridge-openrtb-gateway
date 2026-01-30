package ro.dede.bidbridge.engine.merger;

import org.junit.jupiter.api.Test;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedDevice;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedImp;
import ro.dede.bidbridge.engine.service.AdapterFailureException;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultResponseMergerTest {

    private final DefaultResponseMerger merger = new DefaultResponseMerger(new ResponsePolicy(new ResponseProperties()));

    @Test
    void selectsHighestBid() {
        var results = List.of(
                AdapterResult.bid("a", new SelectedBid("b1", "1", 1.0, "<a/>", "USD"), null),
                AdapterResult.bid("b", new SelectedBid("b2", "1", 2.5, "<b/>", "USD"), null)
        );

        var response = merger.merge(sampleRequest(), results).block();

        assertNotNull(response);
        assertEquals("b2", response.seatbid().getFirst().bid().getFirst().id());
    }

    @Test
    void returnsNoBidWhenNoBids() {
        var results = List.of(
                AdapterResult.noBid("a", null),
                AdapterResult.noBid("b", null)
        );

        var response = merger.merge(sampleRequest(), results).blockOptional();

        assertEquals(true, response.isEmpty());
    }

    @Test
    void ignoresNonPositiveBids() {
        var results = List.of(
                AdapterResult.bid("a", new SelectedBid("b1", "1", 0.0, "<a/>", "USD"), null),
                AdapterResult.noBid("b", null)
        );

        var response = merger.merge(sampleRequest(), results).blockOptional();

        assertEquals(true, response.isEmpty());
    }

    @Test
    void returnsOverloadOnAllTimeouts() {
        var results = List.of(
                new AdapterResult("a", AdapterResultStatus.TIMEOUT, 10L, null, null),
                new AdapterResult("b", AdapterResultStatus.TIMEOUT, 10L, null, null)
        );

        assertThrows(OverloadException.class, () -> merger.merge(sampleRequest(), results).block());
    }

    @Test
    void returnsAdapterFailureOnAllErrors() {
        var results = List.of(
                new AdapterResult("a", AdapterResultStatus.ERROR, 10L, null, null),
                new AdapterResult("b", AdapterResultStatus.ERROR, 10L, null, null)
        );

        assertThrows(AdapterFailureException.class, () -> merger.merge(sampleRequest(), results).block());
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
