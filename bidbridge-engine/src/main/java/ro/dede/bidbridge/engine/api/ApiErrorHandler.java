package ro.dede.bidbridge.engine.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import ro.dede.bidbridge.engine.observability.MetricsCollector;
import ro.dede.bidbridge.engine.observability.RequestOutcome;
import ro.dede.bidbridge.engine.service.AdapterFailureException;
import ro.dede.bidbridge.engine.service.ConfigurationException;
import ro.dede.bidbridge.engine.service.FilteredRequestException;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.stream.Collectors;

// Centralized 400 handling for validation and parse errors.
@RestControllerAdvice
public class ApiErrorHandler {

    private final MetricsCollector metrics;

    public ApiErrorHandler(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        var message = ex.getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        metrics.recordError("validation");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInput(ServerWebInputException ex) {
        var message = extractBadRequestMessage(ex);
        metrics.recordError("input");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    private String extractBadRequestMessage(ServerWebInputException ex) {
        var message = ex.getReason();
        return message == null || message.isBlank()
                ? "Invalid request"
                : message;
    }

    @ExceptionHandler(OverloadException.class)
    public ResponseEntity<Void> handleOverload(OverloadException ex, ServerWebExchange exchange) {
        var outcome = switch (ex.reason()) {
            case REQUEST_DEADLINE_TIMEOUT -> RequestOutcome.NO_BID_TIMEOUT_DEADLINE;
            case ALL_ADAPTERS_TIMED_OUT -> RequestOutcome.NO_BID_TIMEOUT_ADAPTERS;
            case UNKNOWN -> RequestOutcome.NO_BID_TIMEOUT_DEADLINE;
        };
        exchange.getAttributes().put(MetricsCollector.ATTR_REQUEST_OUTCOME, outcome);
        return ResponseEntity.noContent()
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .build();
    }

    @ExceptionHandler(AdapterFailureException.class)
    public ResponseEntity<Void> handleAdapterFailure(AdapterFailureException ex, ServerWebExchange exchange) {
        metrics.recordError("adapter_failure");
        exchange.getAttributes().put(MetricsCollector.ATTR_REQUEST_OUTCOME, RequestOutcome.NO_BID_ADAPTER_FAILURE);
        return ResponseEntity.noContent()
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .build();
    }

    @ExceptionHandler(FilteredRequestException.class)
    public ResponseEntity<Void> handleFilteredRequest(ServerWebExchange exchange) {
        exchange.getAttributes().put(MetricsCollector.ATTR_REQUEST_OUTCOME, RequestOutcome.NO_BID_FILTERED);
        return ResponseEntity.noContent()
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .build();
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfiguration(ConfigurationException ex) {
        var message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Configuration error"
                : ex.getMessage();
        metrics.recordError("configuration");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        metrics.recordError("unexpected");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse("Internal error"));
    }
}
