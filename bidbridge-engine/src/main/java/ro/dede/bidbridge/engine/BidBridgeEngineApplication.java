package ro.dede.bidbridge.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class BidBridgeEngineApplication {

    private static final Logger log =
            LoggerFactory.getLogger(BidBridgeEngineApplication.class);

    private final Environment environment;

    public BidBridgeEngineApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(BidBridgeEngineApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        var appName = environment.getProperty("spring.application.name", "BidBridge Engine");
        var version = environment.getProperty("info.build.version", "unknown");
        var buildTime = environment.getProperty("info.build.time", "unknown");
        var commit = environment.getProperty("info.git.commit.id.abbrev", "unknown");
        var tags = environment.getProperty("info.git.tags", "unknown");
        var profiles = environment.getActiveProfiles();
        var port = environment.getProperty("server.port", "8080");
        var contextPath = environment.getProperty("server.servlet.context-path", "/");

        log.info(
                "{} is ready | version={} | buildTime={} | commit={} | tags={} | profiles={} | port={} | contextPath={}",
                appName,
                version,
                buildTime,
                commit,
                tags,
                Arrays.toString(profiles),
                port,
                contextPath
        );
    }
}
