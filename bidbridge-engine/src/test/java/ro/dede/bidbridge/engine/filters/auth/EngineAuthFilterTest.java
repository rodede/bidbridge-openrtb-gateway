package ro.dede.bidbridge.engine.filters.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.api.ApiErrorHandler;
import ro.dede.bidbridge.engine.api.BidController;
import ro.dede.bidbridge.engine.config.EngineAuthProperties;
import ro.dede.bidbridge.engine.config.EngineLimitsProperties;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.domain.openrtb.Imp;
import ro.dede.bidbridge.engine.normalization.BidRequestNormalizer;
import ro.dede.bidbridge.engine.service.BidService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@WebFluxTest(controllers = BidController.class)
@Import({EngineAuthFilter.class, EngineAuthProperties.class, ApiErrorHandler.class, EngineAuthFilterTest.TestConfig.class})
@ActiveProfiles("aws")
@TestPropertySource(properties = {
        "engine.auth.enabled=true",
        "engine.auth.bidApiKey=test-bid-key"
})
class EngineAuthFilterTest {

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
        bidRequestNormalizer.setNext(Mono.just(sampleNormalizedRequest()));
        bidService.setNext(Mono.empty());
    }

    @Test
    void rejectsBidWithoutApiKey() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void rejectsOpenRtbSubpathWithoutApiKey() {
        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void allowsBidWithApiKey() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new ro.dede.bidbridge.engine.domain.openrtb.Site(null), null, null, null, null, null, null);

        webTestClient.post()
                .uri("/openrtb2/bid")
                .header("X-Api-Key", "test-bid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent();
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

        @Bean
        EngineLimitsProperties engineLimitsProperties() {
            var properties = new EngineLimitsProperties();
            properties.setMaxInFlight(1_000);
            return properties;
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
