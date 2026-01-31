package ro.dede.bidbridge.simulator.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.api.ErrorResponse;
import ro.dede.bidbridge.simulator.config.DspConfigStore;
import ro.dede.bidbridge.simulator.model.BidRequest;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles DSP selection, validation, response decision, and logging.
 */
@Component
public class DspResponseService {
    private static final Logger log = LoggerFactory.getLogger(DspResponseService.class);
    private static final String OPENRTB_VERSION_HEADER = "X-OpenRTB-Version";
    private static final String OPENRTB_VERSION = "2.6";

    private final DspConfigStore configStore;
    private final DspBidder bidder;

    public DspResponseService(DspConfigStore configStore, DspBidder bidder) {
        this.configStore = configStore;
        this.bidder = bidder;
    }

    public Mono<ResponseEntity<?>> handle(String dspName, BidRequest request) {
        if (dspName == null || dspName.isBlank()) {
            log.info("Simulator bid response: status=400 error=missing_dsp");
            return delayed(ResponseEntity.badRequest()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .body(new ErrorResponse("Missing dsp")), 0);
        }
        var config = configStore.getConfig(dspName);
        if (config == null) {
            log.info("Simulator bid response: status=400 error=unknown_dsp dsp={}", dspName);
            return delayed(ResponseEntity.badRequest()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .body(new ErrorResponse("Unknown dsp: " + dspName)), 0);
        }
        if (!config.isEnabled()) {
            log.info("Simulator bid response: status=204 reason=disabled dsp={}", dspName);
            return delayed(ResponseEntity.noContent()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .build(), config.getResponseDelayMs());
        }
        var validationError = validate(request);
        if (validationError != null) {
            log.info("Simulator bid response: status=400 error={} dsp={}", validationError, dspName);
            return delayed(ResponseEntity.badRequest()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .body(new ErrorResponse(validationError)), config.getResponseDelayMs());
        }
        if (ThreadLocalRandom.current().nextDouble() > config.getBidProbability()) {
            log.info("Simulator bid response: status=204 reason=no-bid dsp={}", dspName);
            return delayed(ResponseEntity.noContent()
                    .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                    .build(), config.getResponseDelayMs());
        }
        return bidder.bid(request, config)
                .map(response -> {
                    var impId = request.imp().getFirst().id();
                    log.info("Simulator bid response: status=200 dsp={} id={} impid={} price={} cur={}",
                            dspName, response.id(), impId, response.seatbid().getFirst().bid().getFirst().price(), response.cur());
                    return ResponseEntity.ok()
                            .header(OPENRTB_VERSION_HEADER, OPENRTB_VERSION)
                            .body(response);
                })
                .flatMap(entity -> delayed(entity, config.getResponseDelayMs()));
    }

    private Mono<ResponseEntity<?>> delayed(ResponseEntity<?> response, int delayMs) {
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
