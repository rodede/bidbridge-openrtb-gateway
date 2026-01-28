package ro.dede.bidbridge.simulator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Minimal OpenRTB bidder simulator endpoint.
 */
@RestController
public class SimulatorController {
    private static final String OPENRTB_VERSION_HEADER = "X-OpenRTB-Version";
    private static final String OPENRTB_VERSION = "2.6";

    private final SimulatorProperties properties;

    public SimulatorController(SimulatorProperties properties) {
        this.properties = properties;
    }

    @PostMapping(path = "/openrtb2/bid")
    public Mono<ResponseEntity<?>> bid(@RequestBody BidRequest request) {
        if (!properties.isEnabled()) {
            return delayed(ResponseEntity.noContent().header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION).build());
        }
        var validationError = validate(request);
        if (validationError != null) {
            return delayed(ResponseEntity.badRequest()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .body(new ErrorResponse(validationError)));
        }

        if (ThreadLocalRandom.current().nextDouble() > properties.getBidProbability()) {
            return delayed(ResponseEntity.noContent().header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION).build());
        }

        var impId = request.imp().getFirst().id();
        var bid = new Bid("bid-1", impId, properties.getFixedPrice(), properties.getAdmTemplate());
        var response = new BidResponse(
                request.id(),
                List.of(new SeatBid(List.of(bid))),
                properties.getCurrency()
        );

        return delayed(ResponseEntity.ok()
                .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                .body(response));
    }

    private Mono<ResponseEntity<?>> delayed(ResponseEntity<?> response) {
        var delayMs = properties.getResponseDelayMs();
        if (delayMs <= 0) {
            return Mono.just(response);
        }
        return Mono.delay(Duration.ofMillis(delayMs)).thenReturn(response);
    }

    private String validate(BidRequest request) {
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
