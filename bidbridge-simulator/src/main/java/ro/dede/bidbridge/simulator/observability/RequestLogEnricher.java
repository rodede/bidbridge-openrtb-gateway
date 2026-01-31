package ro.dede.bidbridge.simulator.observability;

import org.springframework.web.server.ServerWebExchange;

/**
 * Populates request log attributes on the exchange for summary logging.
 */
public final class RequestLogEnricher {
    public void captureDsp(ServerWebExchange exchange, String dspName) {
        if (exchange != null && dspName != null && !dspName.isBlank()) {
            exchange.getAttributes().put(RequestLogAttributes.DSP_ID, dspName);
        }
    }

    public void captureLatency(ServerWebExchange exchange, int delayMs) {
        if (exchange != null) {
            exchange.getAttributes().put(RequestLogAttributes.LATENCY_MS, delayMs);
        }
    }

    public void captureError(ServerWebExchange exchange, String type, String message) {
        if (exchange != null) {
            exchange.getAttributes().put(RequestLogAttributes.ERROR_TYPE, type);
            exchange.getAttributes().put(RequestLogAttributes.ERROR_MESSAGE, message);
        }
    }
}
