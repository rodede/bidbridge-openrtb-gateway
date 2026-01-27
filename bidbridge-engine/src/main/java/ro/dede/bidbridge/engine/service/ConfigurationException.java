package ro.dede.bidbridge.engine.service;

/**
 * Signals misconfiguration or missing system wiring.
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }
}
