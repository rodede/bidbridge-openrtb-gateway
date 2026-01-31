package ro.dede.bidbridge.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = SimulatorController.class)
@Import(SimulatorControllerTest.TestConfig.class)
class SimulatorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private InMemoryDspConfigStore configStore;
    private DspConfig config;

    @BeforeEach
    void setUp() {
        config = new DspConfig();
        config.setEnabled(true);
        config.setBidProbability(1.0);
        config.setFixedPrice(1.25);
        config.setCurrency("USD");
        config.setAdmTemplate("<vast/>");
        config.setResponseDelayMs(0);
        configStore.put("simulator", config);
    }

    @Test
    void returnsBidWhenProbabilityIsOne() {
        var request = """
                {"id":"req-1","imp":[{"id":"1"}]}
                """;

        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.id").isEqualTo("req-1")
                .jsonPath("$.seatbid[0].bid[0].impid").isEqualTo("1")
                .jsonPath("$.seatbid[0].bid[0].price").isEqualTo(1.25);
    }

    @Test
    void returnsNoBidWhenProbabilityIsZero() {
        config.setBidProbability(0.0);
        var request = """
                {"id":"req-2","imp":[{"id":"1"}]}
                """;

        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");
    }

    @Test
    void returns400OnInvalidRequest() {
        var request = """
                {"id":"","imp":[]}
                """;

        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        InMemoryDspConfigStore dspConfigStore() {
            return new InMemoryDspConfigStore();
        }

        @Bean
        DspBidder dspBidder() {
            return new DefaultDspBidder();
        }

        @Bean
        DspResponseService dspResponseService(DspConfigStore configStore, DspBidder bidder) {
            return new DspResponseService(configStore, bidder);
        }
    }

    static final class InMemoryDspConfigStore implements DspConfigStore {
        private final java.util.concurrent.ConcurrentHashMap<String, DspConfig> configs =
                new java.util.concurrent.ConcurrentHashMap<>();

        void put(String name, DspConfig config) {
            configs.put(name, config);
        }

        @Override
        public DspConfig getConfig(String dspName) {
            return configs.get(dspName);
        }

        @Override
        public ReloadResult reload() {
            return new ReloadResult(true, configs.size(), 0L, "test");
        }
    }
}
