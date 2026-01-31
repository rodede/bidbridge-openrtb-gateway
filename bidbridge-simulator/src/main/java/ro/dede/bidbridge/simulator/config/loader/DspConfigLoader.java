package ro.dede.bidbridge.simulator.config.loader;

/**
 * Loads and validates DSP configuration files.
 */
public interface DspConfigLoader {
    DspConfigLoadResult load(String location);

    long lastModified(String location);
}
