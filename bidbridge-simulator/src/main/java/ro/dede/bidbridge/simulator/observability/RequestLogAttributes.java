package ro.dede.bidbridge.simulator.observability;

/**
 * Attribute keys used for request logging enrichment.
 */
public final class RequestLogAttributes {
    public static final String DSP_ID = "dspId";
    public static final String ERROR_TYPE = "errorType";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String LATENCY_MS = "latencyMs";

    private RequestLogAttributes() {
    }
}
