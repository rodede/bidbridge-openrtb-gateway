package ro.dede.bidbridge.engine.service;

/**
 * Signals a request-timeout condition treated as no-bid at HTTP layer.
 */
public class OverloadException extends RuntimeException {
    public enum Reason {
        REQUEST_DEADLINE_TIMEOUT,
        ALL_ADAPTERS_TIMED_OUT,
        UNKNOWN
    }

    private final Reason reason;

    public OverloadException(String message) {
        this(message, Reason.UNKNOWN);
    }

    public OverloadException(String message, Reason reason) {
        super(message);
        this.reason = reason == null ? Reason.UNKNOWN : reason;
    }

    public Reason reason() {
        return reason;
    }
}
