package ro.dede.bidbridge.simulator.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.OpenRtbConstants;
import ro.dede.bidbridge.simulator.api.ErrorResponse;
import ro.dede.bidbridge.simulator.config.DspConfigStore;
import ro.dede.bidbridge.simulator.model.BidRequest;
import ro.dede.bidbridge.simulator.observability.RequestLogEnricher;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles DSP selection, validation, and response decision.
 */
@Component
public class DspResponseService {
    private static final Logger log = LoggerFactory.getLogger(DspResponseService.class);

    private final DspConfigStore configStore;
    private final DspBidder bidder;
    private final BidRequestValidator validator;
    private final RequestLogEnricher logEnricher;

    public DspResponseService(DspConfigStore configStore, DspBidder bidder) {
        this.configStore = configStore;
        this.bidder = bidder;
        this.validator = new BidRequestValidator();
        this.logEnricher = new RequestLogEnricher();
    }

    public Mono<ResponseEntity<?>> handle(String dspName, BidRequest request, ServerWebExchange exchange) {
        var start = System.nanoTime();
        logEnricher.captureLatency(exchange, 0);
        if (dspName == null || dspName.isBlank()) {
            log.debug("Simulator bid response: status=400 error=missing_dsp");
            logEnricher.captureError(exchange, "missing_dsp", "Missing dsp");
            return delayed(ResponseEntity.badRequest()
                    .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                    .body(new ErrorResponse("Missing dsp")), 0);
        }
        logEnricher.captureDsp(exchange, dspName);
        var config = configStore.getConfig(dspName);
        if (config == null) {
            log.debug("Simulator bid response: status=400 error=unknown_dsp dsp={}", dspName);
            logEnricher.captureError(exchange, "unknown_dsp", "Unknown dsp: " + dspName);
            return delayed(ResponseEntity.badRequest()
                    .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                    .body(new ErrorResponse("Unknown dsp: " + dspName)), 0);
        }
        logEnricher.captureLatency(exchange, config.getResponseDelayMs());
        if (!config.isEnabled()) {
            log.debug("Simulator bid response: status=204 reason=disabled dsp={}", dspName);
            return delayed(ResponseEntity.noContent()
                    .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                    .build(), config.getResponseDelayMs())
                    .doOnSuccess(response -> logTiming(dspName, config.getResponseDelayMs(), start));
        }
        var validationError = validator.validate(request);
        if (validationError != null) {
            log.debug("Simulator bid response: status=400 error={} dsp={}", validationError, dspName);
            logEnricher.captureError(exchange, "invalid_request", validationError);
            return delayed(ResponseEntity.badRequest()
                    .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                    .body(new ErrorResponse(validationError)), config.getResponseDelayMs())
                    .doOnSuccess(response -> logTiming(dspName, config.getResponseDelayMs(), start));
        }
        if (ThreadLocalRandom.current().nextDouble() > config.getBidProbability()) {
            log.debug("Simulator bid response: status=204 reason=no-bid dsp={}", dspName);
            return delayed(ResponseEntity.noContent()
                    .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                    .build(), config.getResponseDelayMs())
                    .doOnSuccess(response -> logTiming(dspName, config.getResponseDelayMs(), start));
        }
        return bidder.bid(request, config)
                .map(response -> {
                    var impId = request.imp().getFirst().id();
                    log.debug("Simulator bid response: status=200 dsp={} id={} impid={} price={} cur={}",
                            dspName, response.id(), impId, response.seatbid().getFirst().bid().getFirst().price(), response.cur());
                    return ResponseEntity.ok()
                            .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                            .body(response);
                })
                .flatMap(entity -> delayed(entity, config.getResponseDelayMs()))
                .doOnSuccess(response -> logTiming(dspName, config.getResponseDelayMs(), start));
    }

    private Mono<ResponseEntity<?>> delayed(ResponseEntity<?> response, int delayMs) {
        if (delayMs <= 0) {
            return Mono.just(response);
        }
        return Mono.delay(Duration.ofMillis(delayMs)).thenReturn(response);
    }

    private void logTiming(String dspName, int delayMs, long startNanos) {
        var durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.debug("Simulator timing dsp={} latencyMs={} durationMs={}", dspName, delayMs, durationMs);
    }

}
