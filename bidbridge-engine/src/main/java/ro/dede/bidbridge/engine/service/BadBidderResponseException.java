package ro.dede.bidbridge.engine.service;

public class BadBidderResponseException extends RuntimeException {
    public BadBidderResponseException(String message) {
        super(message);
    }

    public BadBidderResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
