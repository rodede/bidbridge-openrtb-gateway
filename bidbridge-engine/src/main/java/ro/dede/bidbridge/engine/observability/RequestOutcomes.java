package ro.dede.bidbridge.engine.observability;

public final class RequestOutcomes {
    public static final String REQUEST_OUTCOME = "requestOutcome";

    public static final String BID = "bid";
    public static final String NO_BID_NO_FILL = "nobid_no_fill";
    public static final String NO_BID_FILTERED = "nobid_filtered";
    public static final String NO_BID_TIMEOUT_DEADLINE = "nobid_timeout_deadline";
    public static final String NO_BID_TIMEOUT_ADAPTERS = "nobid_timeout_adapters";
    public static final String NO_BID_ADAPTER_FAILURE = "nobid_adapter_failure";
    public static final String NO_BID_UNKNOWN = "nobid_unknown";
    public static final String ERROR = "error";
    public static final String UNKNOWN = "unknown";

    private RequestOutcomes() {
    }
}
