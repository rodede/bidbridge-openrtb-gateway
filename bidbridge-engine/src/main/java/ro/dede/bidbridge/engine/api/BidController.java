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
import ro.dede.bidbridge.engine.normalization.BidRequestNormalizer;
import ro.dede.bidbridge.engine.service.BidService;

@RestController
@RequestMapping("/openrtb2")
public class BidController {
    private final BidService bidService;
    private final BidRequestNormalizer bidRequestNormalizer;

    public BidController(BidService bidService, BidRequestNormalizer bidRequestNormalizer) {
        this.bidService = bidService;
        this.bidRequestNormalizer = bidRequestNormalizer;
    }

    @PostMapping("/bid")
    public Mono<ResponseEntity<BidResponse>> bid(@Valid @RequestBody BidRequest request) {
        // Map service outcome to OpenRTB HTTP status codes and include version header.
        return bidRequestNormalizer.normalize(request)
                .flatMap(bidService::bid)
                .map(response -> ResponseEntity.ok()
                        .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                        .body(response))
                .switchIfEmpty(Mono.fromSupplier(() -> ResponseEntity.noContent()
                        .header(OpenRtbConstants.OPENRTB_VERSION_HEADER, OpenRtbConstants.OPENRTB_VERSION)
                        .build()));
    }
}
