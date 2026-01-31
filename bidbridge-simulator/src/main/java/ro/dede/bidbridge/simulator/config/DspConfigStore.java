package ro.dede.bidbridge.simulator.config;

/**
 * Abstraction for loading and serving DSP configuration.
 */
public interface DspConfigStore {
    /**
     * Returns the config for a single DSP or null if missing.
     */
    DspConfig getConfig(String dspName);

    /**
     * Returns the full config map (read-only view).
     */
    java.util.Map<String, DspConfig> allConfigs();

    /**
     * Reloads configs from the backing source, keeping last good config on error.
     */
    ReloadResult reload();

    /**
     * Returns a snapshot of the current config state.
     */
    Snapshot snapshot();

    /**
     * Result of the last reload attempt.
     */
    record ReloadResult(boolean success, int loadedCount, long versionTimestamp, String message) {

    }

    /**
     * Snapshot of current load state.
     */
    record Snapshot(int loadedCount, long versionTimestamp) {

    }
}
