package ro.dede.bidbridge.simulator;

import java.util.Map;

record DspConfigLoadResult(Map<String, DspConfig> configs, long versionTimestamp) {
}
