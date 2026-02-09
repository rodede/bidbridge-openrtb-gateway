package ro.dede.bidbridge.engine.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration-style tests for the HTTP pipeline and response merger behavior.
 */
@SpringBootTest
@AutoConfigureWebTestClient
class BidControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StubBidRequestNormalizer bidRequestNormalizer;

    @Autowired
    private StubBidService bidService;

    @Autowired
    private ResponseMerger responseMerger;

    @BeforeEach
    void setUp() {
        bidRequestNormalizer.reset();
        bidService.reset();
    }

    @Test
    void returns204WhenOnlyInvalidBidsExist() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        var invalid = AdapterResult.bid("sim",
                new SelectedBid("bid-1", "1", 0.0, "<adm/>", "USD"), null);
        bidService.setNext(responseMerger.merge(sampleNormalizedRequest(), List.of(invalid)));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns204OnAdapterFailureNoBid() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.error(new AdapterFailureException("adapter failure")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns204OnTimeoutNoBid() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.error(new OverloadException("overload")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns204WhenFilteredByRules() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.error(new FilteredRequestException("filtered")));

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
            return new StubBidRequestNormalizer();
        }

        @Bean
        @org.springframework.context.annotation.Primary
        BidService bidService() {
            return new StubBidService();
        }

    }

    static final class StubBidService implements BidService {
        private final AtomicReference<Mono<ro.dede.bidbridge.engine.domain.openrtb.BidResponse>> next =
                new AtomicReference<>(Mono.empty());

        void setNext(Mono<ro.dede.bidbridge.engine.domain.openrtb.BidResponse> result) {
            next.set(result);
        }

        void reset() {
            next.set(Mono.empty());
        }

        @Override
        public Mono<ro.dede.bidbridge.engine.domain.openrtb.BidResponse> bid(NormalizedBidRequest request) {
            return next.get();
        }
    }

    static final class StubBidRequestNormalizer implements BidRequestNormalizer {
        private final AtomicReference<Mono<NormalizedBidRequest>> next =
                new AtomicReference<>(Mono.error(new IllegalStateException("not configured")));

        void setNext(Mono<NormalizedBidRequest> result) {
            next.set(result);
        }

        void reset() {
            next.set(Mono.error(new IllegalStateException("not configured")));
        }

        @Override
        public Mono<NormalizedBidRequest> normalize(BidRequest request) {
            return next.get();
        }
    }
}
