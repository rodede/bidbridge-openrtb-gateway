package ro.dede.bidbridge.engine.adapters.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.adapters.AdapterContext;
import ro.dede.bidbridge.engine.adapters.AdapterProperties;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResultStatus;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.domain.openrtb.Bid;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.domain.openrtb.SeatBid;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimulatorHttpAdapterTest {

    @Test
    void returnsBidOnOkResponse() {
        var client = new StubClient(new HttpBidderResponse<>(200,
                new BidResponse("req-1",
                        List.of(new SeatBid(List.of(new Bid("b1", "1", 1.2, "<adm/>")))),
                        "USD"),
                120));
        var adapter = new SimulatorHttpAdapter(client);
        var context = new AdapterContext("simulatorHttp", configWithEndpoint());

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.BID, result.status());
        assertEquals("b1", result.bid().id());
        assertEquals(200, result.debug().httpStatus());
        assertEquals(120, result.debug().responseSize());
    }

    @Test
    void returnsNoBidOn204() {
        var client = new StubClient(new HttpBidderResponse<>(204, null, 0));
        var adapter = new SimulatorHttpAdapter(client);
        var context = new AdapterContext("simulatorHttp", configWithEndpoint());

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.NO_BID, result.status());
        assertEquals(204, result.debug().httpStatus());
    }

    @Test
    void returnsErrorOnNon2xx() {
        var client = new StubClient(new HttpBidderResponse<>(500, null, 10));
        var adapter = new SimulatorHttpAdapter(client);
        var context = new AdapterContext("simulatorHttp", configWithEndpoint());

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.ERROR, result.status());
        assertEquals(500, result.debug().httpStatus());
        assertEquals("http_status", result.debug().errorCode());
    }

    @Test
    void returnsErrorWhenEndpointMissing() {
        var client = new StubClient(new HttpBidderResponse<>(200, null, null));
        var adapter = new SimulatorHttpAdapter(client);
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        var context = new AdapterContext("simulatorHttp", config);

        var result = adapter.bid(sampleRequest(), context).block();

        assertNotNull(result);
        assertEquals(AdapterResultStatus.ERROR, result.status());
        assertEquals("missing_endpoint", result.debug().errorCode());
    }

    private AdapterProperties.AdapterConfig configWithEndpoint() {
        var config = new AdapterProperties.AdapterConfig();
        config.setEnabled(true);
        config.setEndpoint("http://localhost:8081/openrtb2/bid");
        return config;
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

    private record StubClient(HttpBidderResponse<BidResponse> response) implements HttpBidderClient<BidResponse> {
        @Override
        public Mono<HttpBidderResponse<BidResponse>> postJson(String endpoint, Object body) {
            return Mono.just(response);
        }
    }
}
