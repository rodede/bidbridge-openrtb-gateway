package ro.dede.bidbridge.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.domain.openrtb.*;
import ro.dede.bidbridge.engine.normalization.BidRequestNormalizer;
import ro.dede.bidbridge.engine.normalization.InvalidRequestException;
import ro.dede.bidbridge.engine.service.BidService;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(controllers = BidController.class)
@Import({ApiErrorHandler.class, BidControllerTest.TestConfig.class})
class BidControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StubBidService bidService;

    @Autowired
    private StubBidRequestNormalizer bidRequestNormalizer;

    @BeforeEach
    void setUp() {
        bidService.reset();
        bidRequestNormalizer.reset();
    }

    @Test
    void returns200WithBidResponse() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);
        var response = new BidResponse(
                "req-1",
                List.of(new SeatBid(List.of(new Bid("bid-1", "1", 1.23, "<vast/>")))),
                "USD"
        );

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.just(response));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.id").isEqualTo("req-1");
    }

    @Test
    void returns204WhenNoBid() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.empty());

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns400OnValidationError() {
        var request = new BidRequest("", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    void returns503OnOverload() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.error(new OverloadException("overload")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("overload");
    }

    @Test
    void returns400OnNormalizationError() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.error(new InvalidRequestException("bad")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("bad");
    }

    @Test
    void returns500OnUnexpectedError() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.error(new RuntimeException("boom")));

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
        .expectBody()
                .jsonPath("$.error").isEqualTo("Internal error");
    }

    @Test
    void acceptsOpenRtb25SubsetFields() {
        var json = """
                {
                  "id": "req-25",
                  "imp": [
                    {
                      "id": "1",
                      "banner": {"w": 300, "h": 250}
                    }
                  ],
                  "tmax": 120,
                  "site": {"domain": "example.com"},
                  "device": {"ua": "test"},
                  "ext": {"source": "ssp", "nested": {"a": 1}}
                }
                """;

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.empty());

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");

        var captured = bidRequestNormalizer.lastRequest.get();
        assertEquals(120, captured.tmax());
        assertNotNull(captured.ext());
        assertEquals("ssp", captured.ext().get("source"));
    }

    @Test
    void echoesRequestIdAndCallerHeaders() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.empty());

        webTestClient.post()
                .uri("/openrtb2/bid")
                .header("X-Request-Id", "request-123")
                .header("X-Caller", "loadgen-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-Request-Id", "request-123")
                .expectHeader().valueEquals("X-Caller", "loadgen-test");
    }

    private NormalizedBidRequest sampleNormalizedRequest() {
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        BidService bidService() {
            return new StubBidService();
        }

        @Bean
        BidRequestNormalizer requestNormalizer() {
            return new StubBidRequestNormalizer();
        }

        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        @Bean
        ro.dede.bidbridge.engine.observability.MetricsCollector metricsCollector(
                io.micrometer.core.instrument.MeterRegistry registry) {
            return new ro.dede.bidbridge.engine.observability.MetricsCollector(registry);
        }

    }

    static final class StubBidService implements BidService {
        private final AtomicReference<Mono<BidResponse>> next = new AtomicReference<>(Mono.empty());

        void setNext(Mono<BidResponse> result) {
            next.set(result);
        }

        void reset() {
            next.set(Mono.empty());
        }

        @Override
        public Mono<BidResponse> bid(NormalizedBidRequest request) {
            return next.get();
        }
    }

    static final class StubBidRequestNormalizer implements BidRequestNormalizer {
        private final AtomicReference<Mono<NormalizedBidRequest>> next =
                new AtomicReference<>(Mono.error(new IllegalStateException("not configured")));
        private final AtomicReference<BidRequest> lastRequest = new AtomicReference<>();

        void setNext(Mono<NormalizedBidRequest> result) {
            next.set(result);
        }

        void reset() {
            next.set(Mono.error(new IllegalStateException("not configured")));
            lastRequest.set(null);
        }

        @Override
        public Mono<NormalizedBidRequest> normalize(BidRequest request) {
            lastRequest.set(request);
            return next.get();
        }
    }
}
