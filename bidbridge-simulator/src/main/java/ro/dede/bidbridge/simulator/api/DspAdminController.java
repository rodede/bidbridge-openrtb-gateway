package ro.dede.bidbridge.simulator.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.dede.bidbridge.simulator.config.DspConfigStore;

import java.time.Instant;
import java.util.Map;

/**
 * Admin endpoints for DSP config reload and inspection.
 */
@RestController
public class DspAdminController {
    private final DspConfigStore configStore;

    public DspAdminController(DspConfigStore configStore) {
        this.configStore = configStore;
    }

    @PostMapping("/admin/reload-dsps")
    public ResponseEntity<Map<String, Object>> reload() {
        // Always returns 200; status field conveys success/failure.
        var result = configStore.reload();
        var status = result.success() ? "ok" : "error";
        return ResponseEntity.ok(Map.of(
                "status", status,
                "loadedCount", result.loadedCount(),
                "versionTimestamp", result.versionTimestamp(),
                "message", result.message()
        ));
    }

    @GetMapping("/admin/dsps")
    public ResponseEntity<Map<String, Object>> list() {
        // Returns a snapshot view of current configs.
        var snapshot = configStore.snapshot();
        return ResponseEntity.ok(Map.of(
                "loadedCount", snapshot.loadedCount(),
                "versionTimestamp", snapshot.versionTimestamp(),
                "versionTime", Instant.ofEpochMilli(snapshot.versionTimestamp()).toString(),
                "configs", configStore.allConfigs()
        ));
    }
}
