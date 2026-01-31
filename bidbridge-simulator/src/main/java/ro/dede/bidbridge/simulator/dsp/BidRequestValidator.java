package ro.dede.bidbridge.simulator.dsp;

import ro.dede.bidbridge.simulator.model.BidRequest;

/**
 * Minimal validation for OpenRTB requests accepted by the simulator.
 */
public final class BidRequestValidator {
    public String validate(BidRequest request) {
        if (request == null || request.id() == null || request.id().isBlank()) {
            return "id is required";
        }
        if (request.imp() == null || request.imp().isEmpty()) {
            return "imp is required";
        }
        for (var imp : request.imp()) {
            if (imp == null || imp.id() == null || imp.id().isBlank()) {
                return "imp.id is required";
            }
        }
        return null;
    }
}
