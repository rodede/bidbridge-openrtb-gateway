package ro.dede.bidbridge.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimulatorApplication {
    private static final Logger log = LoggerFactory.getLogger(SimulatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
        log.info("BidBridge Simulator is ready");
    }
}
