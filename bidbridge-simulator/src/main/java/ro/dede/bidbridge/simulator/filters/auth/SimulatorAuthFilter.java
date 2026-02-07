package ro.dede.bidbridge.simulator.filters.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.OpenRtbConstants;
import ro.dede.bidbridge.simulator.config.SimulatorAuthProperties;
import ro.dede.bidbridge.simulator.observability.RequestLogEnricher;

import java.nio.charset.StandardCharsets;

/**
 * Shared-secret auth for bid and admin endpoints.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnProperty(prefix = "simulator.auth", name = "enabled", havingValue = "true")
public class SimulatorAuthFilter implements WebFilter {
    private static final String BID_HEADER = "X-Api-Key";
    private static final String ADMIN_HEADER = "X-Admin-Token";
    private static final String ADMIN_PREFIX = "/admin";
    private static final byte[] UNAUTHORIZED_BYTES =
            "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);

    private final String bidApiKey;
    private final String adminApiToken;
    private final RequestLogEnricher logEnricher = new RequestLogEnricher();

    public SimulatorAuthFilter(SimulatorAuthProperties properties) {
        if (!properties.isEnabled()) {
            this.bidApiKey = "";
            this.adminApiToken = "";
            return;
        }
        this.bidApiKey = requireValue(properties.getBidApiKey(), "simulator.auth.bidApiKey");
        this.adminApiToken = requireValue(properties.getAdminApiToken(), "simulator.auth.adminApiToken");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var path = exchange.getRequest().getPath().value();
        if (isOpenRtb(path)) {
            return authorize(exchange, BID_HEADER, bidApiKey, chain);
        }
        if (isAdmin(path)) {
            return authorize(exchange, ADMIN_HEADER, adminApiToken, chain);
        }
        return chain.filter(exchange);
    }

    private boolean isOpenRtb(String path) {
        return path != null && path.startsWith(OpenRtbConstants.OPENRTB_PREFIX);
    }

    private boolean isAdmin(String path) {
        return path != null && (path.equals(ADMIN_PREFIX) || path.startsWith(ADMIN_PREFIX + "/"));
    }

    private Mono<Void> authorize(ServerWebExchange exchange, String headerName, String expected, WebFilterChain chain) {
        var provided = exchange.getRequest().getHeaders().getFirst(headerName);
        if (provided != null && provided.equals(expected)) {
            return chain.filter(exchange);
        }
        logEnricher.captureError(exchange, "unauthorized", "Missing or invalid auth");
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION);
        var buffer = response.bufferFactory().wrap(UNAUTHORIZED_BYTES);
        return response.writeWith(Mono.just(buffer));
    }

    private String requireValue(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be set when simulator.auth.enabled=true");
        }
        return value;
    }
}
