package ro.dede.bidbridge.simulator.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import ro.dede.bidbridge.simulator.config.loader.LocalYamlDspConfigLoader;
import ro.dede.bidbridge.simulator.config.loader.S3YamlDspConfigLoader;

/**
 * Polls dsps.yml at a fixed interval and reloads on change.
 */
@Component
public class DspConfigPoller {
    private static final Logger log = LoggerFactory.getLogger(DspConfigPoller.class);

    private final DspsFileProperties properties;
    private final DspConfigStore configStore;
    private final LocalYamlDspConfigLoader localLoader = new LocalYamlDspConfigLoader();
    private final S3YamlDspConfigLoader s3Loader = new S3YamlDspConfigLoader();
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DspConfigPoller(DspsFileProperties properties, DspConfigStore configStore) {
        this.properties = properties;
        this.configStore = configStore;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "dsps-poller");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PostConstruct
    void start() {
        running.set(true);
        var intervalMs = Math.max(200, properties.getPollIntervalMs());
        scheduler.scheduleWithFixedDelay(this::poll, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("Polling dsps.yml every {}ms", intervalMs);
    }

    @PreDestroy
    void stop() {
        running.set(false);
        scheduler.shutdownNow();
    }

    private void poll() {
        if (!running.get()) {
            return;
        }
        var before = configStore.snapshot().versionTimestamp();
        try {
            var current = lastModified(properties.getFile());
            if (current == before) {
                return;
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to read dsps.yml timestamp: {}", ex.getMessage());
            return;
        }
        var result = configStore.reload();
        if (result.success()) {
            log.info("Reloaded dsps.yml: count={} versionTimestamp={}",
                    result.loadedCount(), result.versionTimestamp());
        } else {
            log.warn("Failed to reload dsps.yml: {}", result.message());
        }
    }

    private long lastModified(String location) {
        if (location.startsWith("s3://")) {
            return s3Loader.lastModified(location);
        }
        return localLoader.lastModified(location);
    }
}
