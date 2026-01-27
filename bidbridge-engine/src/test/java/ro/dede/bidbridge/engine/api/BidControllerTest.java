package ro.dede.bidbridge.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.openrtb.Bid;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.domain.openrtb.Imp;
import ro.dede.bidbridge.engine.domain.openrtb.SeatBid;
import ro.dede.bidbridge.engine.service.BidService;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(controllers = BidController.class)
@Import({ApiErrorHandler.class, BidControllerTest.TestConfig.class})
class BidControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private BidService bidService;

    @BeforeEach
    void setUp() {
        Mockito.reset(bidService);
    }

    @Test
    void returns200WithBidResponse() {
        var request = new BidRequest("req-1", List.of(new Imp("1")), null, null);
        var response = new BidResponse(
                "req-1",
                List.of(new SeatBid(List.of(new Bid("bid-1", "1", 1.23, "<vast/>")))),
                "USD"
        );

        when(bidService.bid(any())).thenReturn(Mono.just(response));

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
        var request = new BidRequest("req-1", List.of(new Imp("1")), null, null);

        when(bidService.bid(any())).thenReturn(Mono.empty());

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
        var request = new BidRequest("", List.of(new Imp("1")), null, null);

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
        var request = new BidRequest("req-1", List.of(new Imp("1")), null, null);

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

        when(bidService.bid(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/openrtb2/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("X-OpenRTB-Version", "2.6");

        var captor = org.mockito.ArgumentCaptor.forClass(BidRequest.class);
        verify(bidService).bid(captor.capture());
        var captured = captor.getValue();
        assertEquals(120, captured.tmax());
        assertNotNull(captured.ext());
        assertEquals("ssp", captured.ext().get("source"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        BidService bidService() {
            return Mockito.mock(BidService.class);
        }
    }
}
