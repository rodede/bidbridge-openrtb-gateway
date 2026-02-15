package ro.dede.bidbridge.engine.api;

public final class OpenRtbConstants {
    public static final String OPENRTB_BASE_PATH = "/openrtb2";
    public static final String OPENRTB_BID_PATH = "/bid";
    public static final String OPENRTB_PREFIX = OPENRTB_BASE_PATH;

    public static final String OPENRTB_VERSION = "2.6";
    public static final String OPENRTB_VERSION_HEADER = "X-OpenRTB-Version";

    public static boolean isOpenRtbBidRequestPath(String path) {
        if (path == null) {
            return false;
        }
        return path.equals(OPENRTB_BASE_PATH) || path.startsWith(OPENRTB_BASE_PATH + "/");
    }

    private OpenRtbConstants() {
    }
}
