package ro.dede.bidbridge.simulator;

import java.nio.file.Path;

/**
 * Loads and validates DSP configuration files.
 */
interface DspConfigLoader {
    DspConfigLoadResult load(Path path);
}
