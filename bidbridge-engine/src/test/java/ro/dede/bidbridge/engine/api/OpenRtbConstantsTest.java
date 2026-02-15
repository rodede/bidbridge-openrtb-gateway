package ro.dede.bidbridge.engine.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRtbConstantsTest {

    @Test
    void matchesOpenRtbBaseAndSubpaths() {
        assertTrue(OpenRtbConstants.isOpenRtbBidRequestPath("/openrtb2"));
        assertTrue(OpenRtbConstants.isOpenRtbBidRequestPath("/openrtb2/bid"));
        assertTrue(OpenRtbConstants.isOpenRtbBidRequestPath("/openrtb2/simulator/bid"));
    }

    @Test
    void rejectsNonOpenRtbPaths() {
        assertFalse(OpenRtbConstants.isOpenRtbBidRequestPath(null));
        assertFalse(OpenRtbConstants.isOpenRtbBidRequestPath("/"));
        assertFalse(OpenRtbConstants.isOpenRtbBidRequestPath("/actuator/health"));
        assertFalse(OpenRtbConstants.isOpenRtbBidRequestPath("/openrtb"));
        assertFalse(OpenRtbConstants.isOpenRtbBidRequestPath("/openrtb20/bid"));
    }
}
