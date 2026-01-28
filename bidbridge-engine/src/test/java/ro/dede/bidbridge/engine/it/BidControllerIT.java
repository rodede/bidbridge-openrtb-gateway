package ro.dede.bidbridge.engine.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.adapter.AdapterResult;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedDevice;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedImp;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.Imp;
import ro.dede.bidbridge.engine.domain.openrtb.Site;
import ro.dede.bidbridge.engine.merger.ResponseMerger;
import ro.dede.bidbridge.engine.normalization.BidRequestNormalizer;
import ro.dede.bidbridge.engine.service.AdapterFailureException;
import ro.dede.bidbridge.engine.service.BidService;
import ro.dede.bidbridge.engine.service.FilteredRequestException;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Integration-style tests for the HTTP pipeline and response merger behavior.
 */
@SpringBootTest
@AutoConfigureWebTestClient
class BidControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private BidRequestNormalizer bidRequestNormalizer;

    @Autowired
    private BidService bidService;

    @Autowired
    private ResponseMerger responseMerger;

    @BeforeEach
    void setUp() {
        reset(bidRequestNormalizer, bidService);
    }

    @Test
    void returns204WhenOnlyInvalidBidsExist() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        when(bidRequestNormalizer.normalize(any())).thenReturn(Mono.just(sampleNormalizedRequest()));
        var invalid = AdapterResult.bid("sim",
                new SelectedBid("bid-1", "1", 0.0, "<adm/>", "USD"), null);
        when(bidService.bid(any())).thenReturn(responseMerger.merge(sampleNormalizedRequest(), List.of(invalid)));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns503OnAdapterFailure() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        when(bidRequestNormalizer.normalize(any())).thenReturn(Mono.just(sampleNormalizedRequest()));
        when(bidService.bid(any())).thenReturn(Mono.error(new AdapterFailureException("adapter failure")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns503OnOverload() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        when(bidRequestNormalizer.normalize(any())).thenReturn(Mono.just(sampleNormalizedRequest()));
        when(bidService.bid(any())).thenReturn(Mono.error(new OverloadException("overload")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns204WhenFilteredByRules() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        when(bidRequestNormalizer.normalize(any())).thenReturn(Mono.just(sampleNormalizedRequest()));
        when(bidService.bid(any())).thenReturn(Mono.error(new FilteredRequestException("filtered")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    private NormalizedBidRequest sampleNormalizedRequest() {
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        BidRequestNormalizer bidRequestNormalizer() {
            return Mockito.mock(BidRequestNormalizer.class);
        }

        @Bean
        @org.springframework.context.annotation.Primary
        BidService bidService() {
            return Mockito.mock(BidService.class);
        }
    }
}
