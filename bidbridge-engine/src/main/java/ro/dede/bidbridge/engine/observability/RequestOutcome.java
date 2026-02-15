package ro.dede.bidbridge.engine.observability;

/**
 * Canonical request outcome values used for logs and metrics tags.
 */
public enum RequestOutcome {
    BID("bid"),
    NO_BID_NO_FILL("nobid_no_fill"),
    NO_BID_FILTERED("nobid_filtered"),
    NO_BID_TIMEOUT_DEADLINE("nobid_timeout_deadline"),
    NO_BID_TIMEOUT_ADAPTERS("nobid_timeout_adapters"),
    NO_BID_ADAPTER_FAILURE("nobid_adapter_failure"),
    NO_BID_UNKNOWN("nobid_unknown"),
    ERROR("error"),
    UNKNOWN("unknown");

    private final String value;

    RequestOutcome(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static RequestOutcome fromStatus(int status) {
        return switch (status) {
            case 200 -> BID;
            case 204 -> NO_BID_UNKNOWN;
            case 0 -> UNKNOWN;
            default -> ERROR;
        };
    }
}
