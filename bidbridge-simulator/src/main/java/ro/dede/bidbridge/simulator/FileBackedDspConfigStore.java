package ro.dede.bidbridge.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * File-backed DSP config store with atomic reload and fallback to last good config.
 */
@Component
public class FileBackedDspConfigStore implements DspConfigStore {
    private static final Logger log = LoggerFactory.getLogger(FileBackedDspConfigStore.class);

    private final DspsFileProperties properties;
    private final DspConfigLoader loader;
    private final AtomicReference<State> state = new AtomicReference<>(new State(Collections.emptyMap(), 0L));

    public FileBackedDspConfigStore(DspsFileProperties properties, DspConfigLoader loader) {
        this.properties = properties;
        this.loader = loader;
    }

    // Initial load on startup to seed the config store.
    @PostConstruct
    void init() {
        reload();
    }

    @Override
    public DspConfig getConfig(String dspName) {
        return state.get().configs.get(dspName);
    }

    @Override
    public ReloadResult reload() {
        try {
            var path = Path.of(properties.getFile());
            var result = loader.load(path);
            var newState = new State(result.configs(), result.versionTimestamp());
            state.set(newState);
            log.info("Loaded dsps.yml: count={} versionTimestamp={}", newState.configs.size(), newState.versionTimestamp);
            return new ReloadResult(true, newState.configs.size(), newState.versionTimestamp, "ok");
        } catch (RuntimeException ex) {
            var current = state.get();
            log.warn("Failed to reload dsps.yml, keeping previous config: {}", ex.getMessage());
            return new ReloadResult(false, current.configs.size(), current.versionTimestamp, ex.getMessage());
        }
    }

    private record State(Map<String, DspConfig> configs, long versionTimestamp) {
    }
}
