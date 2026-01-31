package ro.dede.bidbridge.simulator.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ro.dede.bidbridge.simulator.config.DspConfigStore;
import ro.dede.bidbridge.simulator.config.DspsFileProperties;

/**
 * Logs a concise startup summary for operational visibility.
 */
@Component
public class StartupLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment environment;
    private final DspConfigStore configStore;
    private final DspsFileProperties properties;

    public StartupLogger(Environment environment, DspConfigStore configStore, DspsFileProperties properties) {
        this.environment = environment;
        this.configStore = configStore;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        var profiles = environment.getActiveProfiles();
        var profilesLabel = profiles.length == 0
                ? String.join(",", environment.getDefaultProfiles())
                : String.join(",", profiles);
        var snapshot = configStore.snapshot();
        var pollIntervalMs = properties.getPollIntervalMs();
        var hotReloadEnabled = pollIntervalMs > 0;

        log.info("Startup: profiles={} dspsLoaded={} dspsVersionTs={} hotReload={} pollIntervalMs={} dspsFile={}",
                profilesLabel,
                snapshot.loadedCount(),
                snapshot.versionTimestamp(),
                hotReloadEnabled,
                pollIntervalMs,
                properties.getFile());
    }
}
