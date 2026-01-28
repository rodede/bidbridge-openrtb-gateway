package ro.dede.bidbridge.engine.adapters.http;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;

/**
 * WebClient-based implementation for BidResponse handling.
 */
@Component
public class WebClientBidderClient implements HttpBidderClient<BidResponse> {
    private final WebClient webClient;

    public WebClientBidderClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public Mono<HttpBidderResponse<BidResponse>> postJson(String endpoint, Object body) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> {
                    var status = response.statusCode().value();
                    var length = response.headers().contentLength().orElse(-1L);
                    Integer responseSize = length >= 0
                            ? (int) Math.min(length, Integer.MAX_VALUE)
                            : null;
                    if (status == 204) {
                        return Mono.just(new HttpBidderResponse<>(status, null, responseSize));
                    }
                    return response.bodyToMono(BidResponse.class)
                            .defaultIfEmpty(null)
                            .map(parsed -> new HttpBidderResponse<>(status, parsed, responseSize));
                });
    }
}
