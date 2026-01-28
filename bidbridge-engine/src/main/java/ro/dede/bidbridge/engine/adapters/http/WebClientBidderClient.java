package ro.dede.bidbridge.engine.adapters.http;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.service.BadBidderResponseException;

/**
 * WebClient-based implementation for BidResponse handling.
 */
@Component
public class WebClientBidderClient implements HttpBidderClient<BidResponse> {
    private final WebClient webClient;
    private final Validator validator;

    public WebClientBidderClient(WebClient.Builder builder, Validator validator) {
        this.webClient = builder.build();
        this.validator = validator;
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
                    if (status >= 400) {
                        return Mono.error(new BadBidderResponseException("Bidder returned HTTP " + status));
                    }
                    return response.bodyToMono(BidResponse.class)
                            .map(this::validateResponse)
                            .onErrorMap(DecodingException.class,
                                    ex -> new BadBidderResponseException("Invalid bidder response JSON", ex))
                            .onErrorMap(ConstraintViolationException.class,
                                    ex -> new BadBidderResponseException("Invalid bidder response", ex))
                            .map(parsed -> new HttpBidderResponse<>(status, parsed, responseSize))
                            .switchIfEmpty(Mono.just(new HttpBidderResponse<>(status, null, responseSize)));
                });
    }

    private BidResponse validateResponse(BidResponse response) {
        var violations = validator.validate(response);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return response;
    }
}
