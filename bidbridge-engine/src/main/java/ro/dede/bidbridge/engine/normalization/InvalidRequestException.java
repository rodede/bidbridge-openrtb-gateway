package ro.dede.bidbridge.engine.normalization;

/**
 * Raised when normalization detects an invalid request beyond schema validation.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
