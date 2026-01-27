package ro.dede.bidbridge.engine.domain.adapter;

// Lightweight debug info; avoid raw response bodies.
public record AdapterDebug(
        Integer httpStatus,
        Integer responseSize,
        String errorCode,
        String errorMessage
) {
}
