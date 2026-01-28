package ro.dede.bidbridge.engine.adapters.http;

import org.springframework.stereotype.Component;
import ro.dede.bidbridge.engine.domain.adapter.SelectedBid;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.normalized.NormalizedBidRequest;
import ro.dede.bidbridge.engine.domain.openrtb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter that calls the bidbridge-simulator service.
 */
@Component("simulatorHttp")
public class SimulatorHttpAdapter extends AbstractHttpAdapter<BidRequest, BidResponse> {

    public SimulatorHttpAdapter(HttpBidderClient<BidResponse> client) {
        super(client);
    }

    @Override
    protected BidRequest buildRequest(NormalizedBidRequest request) {
        var imps = new ArrayList<Imp>(request.imps().size());
        for (var imp : request.imps()) {
            var banner = imp.type() == ImpType.BANNER ? new Banner(Map.of()) : null;
            var video = imp.type() == ImpType.VIDEO ? new Video(Map.of()) : null;
            var audio = imp.type() == ImpType.AUDIO ? new Audio(Map.of()) : null;
            var nativeObject = imp.type() == ImpType.NATIVE ? new Native(Map.of()) : null;
            imps.add(new Imp(imp.id(), banner, video, audio, nativeObject, imp.bidfloor(), imp.ext()));
        }

        var site = request.inventoryType() == InventoryType.SITE ? new Site(request.siteExt()) : null;
        var app = request.inventoryType() == InventoryType.APP ? new App(request.appExt()) : null;
        var device = request.device() == null ? null : new Device(
                request.device().ua(),
                request.device().ip(),
                request.device().os(),
                request.device().devicetype(),
                request.device().ext()
        );
        var user = request.userExt() == null ? null : new User(request.userExt());
        var regs = request.regsExt() == null ? null : new Regs(request.regsExt());

        return new BidRequest(
                request.requestId(),
                imps,
                site,
                app,
                device,
                user,
                regs,
                request.tmaxMs(),
                request.ext()
        );
    }

    @Override
    protected SelectedBid extractBid(NormalizedBidRequest request, BidResponse response) {
        if (response == null || response.seatbid() == null) {
            return null;
        }
        Bid best = null;
        for (var seatBid : response.seatbid()) {
            var bids = seatBid == null ? null : seatBid.bid();
            if (bids == null) {
                continue;
            }
            for (var bid : bids) {
                if (bid == null) {
                    continue;
                }
                if (best == null || bid.price() > best.price()) {
                    best = bid;
                }
            }
        }
        if (best == null || best.price() <= 0) {
            return null;
        }
        var currency = response.cur() == null ? "USD" : response.cur();
        return new SelectedBid(best.id(), best.impid(), best.price(), best.adm(), currency);
    }
}
