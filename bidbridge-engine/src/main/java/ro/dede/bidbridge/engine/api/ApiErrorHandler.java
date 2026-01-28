package ro.dede.bidbridge.engine.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import ro.dede.bidbridge.engine.normalization.InvalidRequestException;
import ro.dede.bidbridge.engine.service.AdapterFailureException;
import ro.dede.bidbridge.engine.service.ConfigurationException;
import ro.dede.bidbridge.engine.service.FilteredRequestException;
import ro.dede.bidbridge.engine.service.OverloadException;

import java.util.stream.Collectors;

// Centralized 400 handling for validation and parse errors.
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        var message = ex.getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInput(ServerWebInputException ex) {
        var message = ex.getReason() == null || ex.getReason().isBlank()
                ? "Invalid request"
                : ex.getReason();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        var message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Invalid request"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(OverloadException.class)
    public ResponseEntity<ErrorResponse> handleOverload(OverloadException ex) {
        var message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Overloaded"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(AdapterFailureException.class)
    public ResponseEntity<ErrorResponse> handleAdapterFailure(AdapterFailureException ex) {
        var message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Adapter failure"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(FilteredRequestException.class)
    public ResponseEntity<Void> handleFilteredRequest(FilteredRequestException ex) {
        return ResponseEntity.noContent()
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .build();
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfiguration(ConfigurationException ex) {
        var message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Configuration error"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                .body(new ErrorResponse("Internal error: " + ex.getMessage()));
    }
}
