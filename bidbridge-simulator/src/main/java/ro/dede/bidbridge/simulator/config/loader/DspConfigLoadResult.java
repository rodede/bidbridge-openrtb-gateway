package ro.dede.bidbridge.simulator.config.loader;

import ro.dede.bidbridge.simulator.config.DspConfig;

import java.util.Map;

public record DspConfigLoadResult(Map<String, DspConfig> configs, long versionTimestamp) {
}
