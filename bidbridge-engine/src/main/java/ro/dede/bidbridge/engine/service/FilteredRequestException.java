package ro.dede.bidbridge.engine.service;

/**
 * Signals a valid no-bid outcome after applying business rules.
 */
public class FilteredRequestException extends RuntimeException {
    public FilteredRequestException(String message) {
        super(message);
    }
}
