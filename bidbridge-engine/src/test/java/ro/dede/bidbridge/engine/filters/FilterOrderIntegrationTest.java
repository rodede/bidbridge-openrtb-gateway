package ro.dede.bidbridge.engine.filters;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.normalization.BidRequestNormalizer;
import ro.dede.bidbridge.engine.service.BidService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("aws")
@TestPropertySource(properties = {
        "engine.auth.enabled=true",
        "engine.auth.bidApiKey=test-bid-key",
        "engine.limits.maxInFlight=1"
})
class FilterOrderIntegrationTest {

    @Autowired
    private DelayingBidService delayingBidService;

    @LocalServerPort
    private int port;

    @Test
    void unauthorizedRequestIsRejectedBeforeInFlightLimit() {
        var requestBody = """
                {
                  "id": "req-1",
                  "imp": [
                    {
                      "id": "1",
                      "banner": {}
                    }
                  ],
                  "site": {}
                }
                """;

        WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build()
                .post()
                .uri("/openrtb2/bid")
                .header("X-Api-Key", "test-bid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity()
                .subscribe();

        assertTrue(delayingBidService.awaitFirstInvocation(2, TimeUnit.SECONDS));

        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        DelayingBidService delayingBidService() {
            return new DelayingBidService();
        }

        @Bean
        @Primary
        BidService bidService(DelayingBidService bidService) {
            return bidService;
        }

        @Bean
        @Primary
        BidRequestNormalizer bidRequestNormalizer() {
            return request -> Mono.just(sampleNormalizedRequest());
        }

        private static NormalizedBidRequest sampleNormalizedRequest() {
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

    static final class DelayingBidService implements BidService {
        private final CountDownLatch firstInvocation = new CountDownLatch(1);

        @Override
        public Mono<ro.dede.bidbridge.engine.domain.openrtb.BidResponse> bid(NormalizedBidRequest request) {
            firstInvocation.countDown();
            return Mono.delay(Duration.ofMillis(500)).then(Mono.empty());
        }

        boolean awaitFirstInvocation(long timeout, TimeUnit unit) {
            try {
                return firstInvocation.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
