package ro.dede.bidbridge.engine.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

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
}
