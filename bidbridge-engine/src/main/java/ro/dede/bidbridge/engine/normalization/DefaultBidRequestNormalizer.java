package ro.dede.bidbridge.engine.normalization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ro.dede.bidbridge.engine.domain.normalized.*;
import ro.dede.bidbridge.engine.domain.openrtb.BidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.Imp;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultBidRequestNormalizer implements BidRequestNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DefaultBidRequestNormalizer.class);
    private static final int TMAX_DEFAULT_MS = 100;
    private static final int TMAX_MIN_MS = 10;
    private static final int TMAX_MAX_MS = 60000;

    @Override
    public Mono<NormalizedBidRequest> normalize(BidRequest request) {
        try {
            return Mono.just(normalizeSync(request));
        } catch (RuntimeException ex) {
            return Mono.error(ex);
        }
    }

    private NormalizedBidRequest normalizeSync(BidRequest request) {
        var inventoryType = deriveInventoryType(request);
        var tmaxMs = clampTmax(request.tmax());
        var device = normalizeDevice(request);
        var imps = normalizeImps(request.imp());

        return new NormalizedBidRequest(
                request.id(),
                imps,
                inventoryType,
                tmaxMs,
                device,
                request.ext(),
                request.site() == null ? null : request.site().ext(),
                request.app() == null ? null : request.app().ext(),
                request.user() == null ? null : request.user().ext(),
                request.regs() == null ? null : request.regs().ext()
        );
    }

    private InventoryType deriveInventoryType(BidRequest request) {
        var hasSite = request.site() != null;
        var hasApp = request.app() != null;
        if (hasSite == hasApp) {
            throw new InvalidRequestException("Exactly one of site or app must be present");
        }
        return hasSite ? InventoryType.SITE : InventoryType.APP;
    }

    private int clampTmax(Integer tmax) {
        return switch (tmax) {
            case null -> TMAX_DEFAULT_MS;
            case Integer v when v < TMAX_MIN_MS -> TMAX_MIN_MS;
            case Integer v when v > TMAX_MAX_MS -> TMAX_MAX_MS;
            default -> tmax;
        };
    }

    private NormalizedDevice normalizeDevice(BidRequest request) {
        if (request.device() == null) {
            return null;
        }
        var device = request.device();
        return new NormalizedDevice(
                device.ua(),
                device.ip(),
                device.os(),
                device.devicetype(),
                device.ext()
        );
    }

    private List<NormalizedImp> normalizeImps(List<Imp> imps) {
        var normalized = new ArrayList<NormalizedImp>(imps.size());
        for (var imp : imps) {
            var type = deriveImpType(imp);
            var bidfloor = imp.bidfloor() == null ? 0.0 : imp.bidfloor();
            normalized.add(new NormalizedImp(imp.id(), type, bidfloor, imp.ext()));
        }
        return normalized;
    }

    private ImpType deriveImpType(Imp imp) {
        var hasVideo = imp.video() != null;
        var hasAudio = imp.audio() != null;
        var hasBanner = imp.banner() != null;
        var hasNative = imp.nativeObject() != null;
        if (!hasVideo && !hasAudio && !hasBanner && !hasNative) {
            throw new InvalidRequestException("Each imp must include one of banner, video, audio, or native");
        }
        if ((hasVideo && hasAudio) || (hasVideo && hasBanner) || (hasVideo && hasNative)
                || (hasAudio && hasBanner) || (hasAudio && hasNative) || (hasBanner && hasNative)) {
            var chosen = chooseImpType(hasVideo, hasAudio, hasBanner, hasNative);
            log.debug("Multiple imp types present for impId={}, choosing {}", imp.id(), chosen);
            return chosen;
        }
        return chooseImpType(hasVideo, hasAudio, hasBanner, hasNative);
    }

    private ImpType chooseImpType(boolean hasVideo, boolean hasAudio, boolean hasBanner, boolean hasNative) {
        if (hasVideo) {
            return ImpType.VIDEO;
        }
        if (hasAudio) {
            return ImpType.AUDIO;
        }
        if (hasBanner) {
            return ImpType.BANNER;
        }
        return ImpType.NATIVE;
    }
}
