package ro.dede.bidbridge.engine.domain.openrtb;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BidRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMissingId() {
        var request = new BidRequest("", List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);
        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    void rejectsNullId() {
        var request = new BidRequest(null, List.of(new Imp("1", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);
        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    void rejectsEmptyImp() {
        var request = new BidRequest("req-1", List.of(), new Site(null), null, null, null, null, null, null);
        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("imp")));
    }

    @Test
    void rejectsNullImp() {
        var request = new BidRequest("req-1", null, new Site(null), null, null, null, null, null, null);
        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("imp")));
    }

    @Test
    void rejectsBlankImpId() {
        var request = new BidRequest("req-1", List.of(new Imp("", null, null, null, null, null, null)),
                new Site(null), null, null, null, null, null, null);
        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("imp")));
    }
}
