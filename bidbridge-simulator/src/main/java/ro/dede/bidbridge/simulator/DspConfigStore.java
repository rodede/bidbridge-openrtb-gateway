package ro.dede.bidbridge.simulator;

/**
 * Abstraction for loading and serving DSP configuration.
 */
public interface DspConfigStore {
    DspConfig getConfig(String dspName);

    ReloadResult reload();

    record ReloadResult(boolean success, int loadedCount, long versionTimestamp, String message) {
    }
}
