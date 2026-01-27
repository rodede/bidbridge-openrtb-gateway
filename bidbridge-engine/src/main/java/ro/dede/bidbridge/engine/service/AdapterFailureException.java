package ro.dede.bidbridge.engine.service;

/**
 * Signals that all adapters failed (non-timeout errors).
 */
public class AdapterFailureException extends RuntimeException {
    public AdapterFailureException(String message) {
        super(message);
    }
}
