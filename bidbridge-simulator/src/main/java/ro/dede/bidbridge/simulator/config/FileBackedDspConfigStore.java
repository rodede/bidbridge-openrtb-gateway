package ro.dede.bidbridge.simulator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import ro.dede.bidbridge.simulator.config.loader.DspConfigLoadResult;
import ro.dede.bidbridge.simulator.config.loader.LocalYamlDspConfigLoader;
import ro.dede.bidbridge.simulator.config.loader.S3YamlDspConfigLoader;

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
    private final LocalYamlDspConfigLoader localLoader = new LocalYamlDspConfigLoader();
    private final S3YamlDspConfigLoader s3Loader = new S3YamlDspConfigLoader();
    private final AtomicReference<State> state = new AtomicReference<>(new State(Collections.emptyMap(), 0L));

    public FileBackedDspConfigStore(DspsFileProperties properties) {
        this.properties = properties;
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
    public Map<String, DspConfig> allConfigs() {
        return state.get().configs;
    }

    @Override
    // Reloads the file and swaps the config atomically on success.
    public ReloadResult reload() {
        try {
            var result = load(properties.getFile());
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

    private DspConfigLoadResult load(String location) {
        if (location.startsWith("s3://")) {
            return s3Loader.load(location);
        }
        return localLoader.load(location);
    }

    @Override
    public Snapshot snapshot() {
        var current = state.get();
        return new Snapshot(current.configs.size(), current.versionTimestamp);
    }

    private record State(Map<String, DspConfig> configs, long versionTimestamp) {
    }
}
