package ro.dede.bidbridge.engine.normalization;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebInputException;
import ro.dede.bidbridge.engine.domain.normalized.ImpType;
import ro.dede.bidbridge.engine.domain.normalized.InventoryType;
import ro.dede.bidbridge.engine.domain.openrtb.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBidRequestNormalizerTest {

    private final DefaultBidRequestNormalizer normalizer = new DefaultBidRequestNormalizer();

    @Test
    void failsWhenNeitherSiteNorAppPresent() {
        var request = new BidRequest("req-1", List.of(new Imp("1", new Banner(null), null, null, null, null, null)),
                null, null, null, null, null, null, null);

        assertThrows(ServerWebInputException.class, () -> normalizer.normalize(request).block());
    }

    @Test
    void failsWhenBothSiteAndAppPresent() {
        var request = new BidRequest("req-1", List.of(new Imp("1", new Banner(null), null, null, null, null, null)),
                new Site(null), new App(null), null, null, null, null, null);

        assertThrows(ServerWebInputException.class, () -> normalizer.normalize(request).block());
    }

    @Test
    void failsWhenImpHasNoType() {
        var request = new BidRequest("req-1", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);

        assertThrows(ServerWebInputException.class, () -> normalizer.normalize(request).block());
    }

    @Test
    void derivesImpTypeWithPriority() {
        var request = new BidRequest("req-1",
                List.of(new Imp("1", new Banner(null), new Video(null), new Audio(null), new Native(null), null, null)),
                new Site(null), null, null, null, null, null, null);

        var normalized = normalizer.normalize(request).block();

        assertNotNull(normalized);
        assertEquals(ImpType.VIDEO, normalized.imps().getFirst().type());
    }

    @Test
    void appliesDefaultsAndClampsTmax() {
        var defaultRequest = new BidRequest("req-1",
                List.of(new Imp("1", new Banner(null), null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);
        var defaultNormalized = normalizer.normalize(defaultRequest).block();

        assertNotNull(defaultNormalized);
        assertEquals(100, defaultNormalized.tmaxMs());

        var lowRequest = new BidRequest("req-1",
                List.of(new Imp("1", new Banner(null), null, null, null, null, null)),
                new Site(null), null, null, null, null, 5, null);
        var lowNormalized = normalizer.normalize(lowRequest).block();

        assertNotNull(lowNormalized);
        assertEquals(10, lowNormalized.tmaxMs());

        var highRequest = new BidRequest("req-1",
                List.of(new Imp("1", new Banner(null), null, null, null, null, null)),
                new Site(null), null, null, null, null, 5000, null);
        var highNormalized = normalizer.normalize(highRequest).block();

        assertNotNull(highNormalized);
        assertEquals(5000, highNormalized.tmaxMs());
    }

    @Test
    void preservesExtAndDerivesContext() {
        Map<String, Object> deviceExt = Map.of("carrier", "test");
        Map<String, Object> userExt = Map.of("consent", "abc");
        Map<String, Object> regsExt = Map.of("gdpr", 1);
        Map<String, Object> impExt = Map.of("impExt", true);
        Map<String, Object> topExt = Map.of("source", "ssp");

        var request = new BidRequest(
                "req-1",
                List.of(new Imp("1", new Banner(null), null, null, null, null, impExt)),
                new Site(Map.of("siteExt", "x")),
                null,
                new Device("ua", "ip", "os", 1, deviceExt),
                new User(userExt),
                new Regs(regsExt),
                120,
                topExt
        );

        var normalized = normalizer.normalize(request).block();

        assertNotNull(normalized);
        assertEquals(InventoryType.SITE, normalized.inventoryType());
        assertEquals(120, normalized.tmaxMs());
        assertEquals(topExt, normalized.ext());
        assertEquals(impExt, normalized.imps().getFirst().ext());
        assertEquals(deviceExt, normalized.device().ext());
        assertEquals(userExt, normalized.userExt());
        assertEquals(regsExt, normalized.regsExt());
        assertNotNull(normalized.siteExt());
        assertNull(normalized.appExt());
    }
}
