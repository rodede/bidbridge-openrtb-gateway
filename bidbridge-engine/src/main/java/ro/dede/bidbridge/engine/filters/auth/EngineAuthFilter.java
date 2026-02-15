package ro.dede.bidbridge.engine.filters.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.api.OpenRtbConstants;
import ro.dede.bidbridge.engine.config.EngineAuthProperties;

import java.nio.charset.StandardCharsets;

/**
 * Shared-secret auth for engine bidder endpoint.
 */
@Component
@Profile("aws")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnProperty(prefix = "engine.auth", name = "enabled", havingValue = "true")
public class EngineAuthFilter implements WebFilter {
    private static final String BID_HEADER = "X-Api-Key";
    private static final byte[] UNAUTHORIZED_BYTES =
            "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);

    private final String bidApiKey;

    public EngineAuthFilter(EngineAuthProperties properties) {
        if (!properties.isEnabled()) {
            this.bidApiKey = "";
            return;
        }

        if (properties.getBidApiKey() == null || properties.getBidApiKey().isBlank()) {
            throw new IllegalStateException(" engine.auth.bidApiKey must be set when engine.auth.enabled=true");
        }
        this.bidApiKey = properties.getBidApiKey();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!OpenRtbConstants.isOpenRtbBidRequestPath(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        var provided = exchange.getRequest().getHeaders().getFirst(BID_HEADER);
        if (provided != null && provided.equals(bidApiKey)) {
            return chain.filter(exchange);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION);
        var buffer = response.bufferFactory().wrap(UNAUTHORIZED_BYTES);
        return response.writeWith(Mono.just(buffer));
    }

}
