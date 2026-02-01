package ro.dede.bidbridge.simulator.filters.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ro.dede.bidbridge.simulator.api.DspAdminController;
import ro.dede.bidbridge.simulator.api.SimulatorController;
import ro.dede.bidbridge.simulator.api.SimulatorErrorHandler;
import ro.dede.bidbridge.simulator.config.DspConfig;
import ro.dede.bidbridge.simulator.config.DspConfigStore;
import ro.dede.bidbridge.simulator.config.SimulatorAuthProperties;
import ro.dede.bidbridge.simulator.config.SimulatorLimitsProperties;
import ro.dede.bidbridge.simulator.dsp.DefaultDspBidder;
import ro.dede.bidbridge.simulator.dsp.DspBidder;
import ro.dede.bidbridge.simulator.dsp.DspResponseService;
import ro.dede.bidbridge.simulator.filters.observability.RequestLoggingFilter;

@WebFluxTest(controllers = {SimulatorController.class, DspAdminController.class})
@Import({SimulatorAuthFilter.class, SimulatorAuthProperties.class, SimulatorErrorHandler.class, SimulatorAuthFilterTest.TestConfig.class})
@TestPropertySource(properties = {
        "simulator.auth.enabled=true",
        "simulator.auth.bidApiKey=test-bid-key",
        "simulator.auth.adminApiToken=test-admin-token"
})
class SimulatorAuthFilterTest {

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
    void rejectsBidWithoutApiKey() {
        var request = """
                {"id":"req-1","imp":[{"id":"1"}]}
                """;

        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void allowsBidWithApiKey() {
        var request = """
                {"id":"req-1","imp":[{"id":"1"}]}
                """;

        webTestClient.post()
                .uri("/openrtb2/simulator/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "test-bid-key")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void rejectsAdminWithoutToken() {
        webTestClient.get()
                .uri("/admin/dsps")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void allowsAdminWithToken() {
        webTestClient.get()
                .uri("/admin/dsps")
                .header("X-Admin-Token", "test-admin-token")
                .exchange()
                .expectStatus().isOk();
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

        @Bean
        RequestLoggingFilter requestLoggingFilter() {
            return new RequestLoggingFilter(meterRegistry());
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        SimulatorLimitsProperties simulatorLimitsProperties() {
            var properties = new SimulatorLimitsProperties();
            properties.setMaxInFlight(1000);
            return properties;
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
        public java.util.Map<String, DspConfig> allConfigs() {
            return java.util.Map.copyOf(configs);
        }

        @Override
        public ReloadResult reload() {
            return new ReloadResult(true, configs.size(), 0L, "test");
        }

        @Override
        public Snapshot snapshot() {
            return new Snapshot(configs.size(), 0L);
        }
    }
}
