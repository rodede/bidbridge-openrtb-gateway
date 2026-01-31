package ro.dede.bidbridge.simulator.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.codec.DecodingException;
import ro.dede.bidbridge.simulator.observability.RequestLogEnricher;

/**
 * Centralized error handling to avoid leaking stack traces in responses.
 */
@RestControllerAdvice
public class SimulatorErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(SimulatorErrorHandler.class);
    private static final String OPENRTB_VERSION_HEADER = "X-OpenRTB-Version";
    private static final String OPENRTB_VERSION = "2.6";

    private final RequestLogEnricher logEnricher = new RequestLogEnricher();

    @ExceptionHandler({ServerWebInputException.class, WebExchangeBindException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, ServerWebExchange exchange) {
        log.warn("Bad request: {}", ex.getMessage());
        logEnricher.captureError(exchange, "bad_request", safeMessage(ex.getMessage(), "Bad request"));
        return error(HttpStatus.BAD_REQUEST, "Bad request");
    }

    @ExceptionHandler(DecodingException.class)
    public ResponseEntity<ErrorResponse> handleDecoding(DecodingException ex, ServerWebExchange exchange) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        logEnricher.captureError(exchange, "decode_error", "Malformed JSON");
        return error(HttpStatus.BAD_REQUEST, "Malformed JSON");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        var status = ex.getStatusCode();
        var reason = safeMessage(ex.getReason(), "Request failed");
        log.warn("Request failed: status={} reason={}", status.value(), reason);
        logEnricher.captureError(exchange, "status_exception", reason);
        return errorStatus(status, reason);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled error", ex);
        logEnricher.captureError(exchange, "internal_error", "Internal error");
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    private ResponseEntity<ErrorResponse> errorStatus(org.springframework.http.HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    private String safeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }
}
