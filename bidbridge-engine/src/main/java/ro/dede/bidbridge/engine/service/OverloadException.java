package ro.dede.bidbridge.engine.service;

/**
 * Signals an overload condition where the gateway should return HTTP 503.
 */
public class OverloadException extends RuntimeException {
    public OverloadException(String message) {
        super(message);
    }
}
