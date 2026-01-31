package ro.dede.bidbridge.simulator.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.simulator.dsp.DspResponseService;
import ro.dede.bidbridge.simulator.model.BidRequest;

/**
 * OpenRTB simulator endpoint that dispatches per-DSP behavior based on path.
 */
@RestController
public class SimulatorController {
    private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);

    private final DspResponseService responseService;

    public SimulatorController(DspResponseService responseService) {
        this.responseService = responseService;
    }

    @PostMapping(path = "/openrtb2/{dsp}/bid")
    public Mono<org.springframework.http.ResponseEntity<?>> bid(@PathVariable(name = "dsp", required = false) String dsp,
                                                                @RequestBody BidRequest request,
                                                                ServerWebExchange exchange) {
        log.debug("Simulator bid request received for {}: id={}, imps={}",
                dsp,
                request == null ? null : request.id(),
                request == null || request.imp() == null ? null : request.imp().size());
        return responseService.handle(dsp, request, exchange);
    }
}
