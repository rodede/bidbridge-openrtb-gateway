package ro.dede.bidbridge.engine.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.BidResponse;
import ro.dede.bidbridge.engine.service.BidService;
import ro.dede.bidbridge.engine.service.OverloadException;

@RestController
@RequestMapping("/openrtb2")
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/bid")
    public Mono<ResponseEntity<BidResponse>> bid(@Valid @RequestBody BidRequest request) {
        // Map service outcome to OpenRTB HTTP status codes and include version header.
        return bidService.bid(request)
                .map(response -> ResponseEntity.ok()
                        .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                        .body(response))
                .switchIfEmpty(Mono.fromSupplier(() -> ResponseEntity.noContent()
                        .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                        .build()))
                .onErrorResume(OverloadException.class, ex -> Mono.just(ResponseEntity.status(503)
                        .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                        .build()));
    }
}
