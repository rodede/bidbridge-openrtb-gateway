package ro.dede.bidbridge.simulator.config.loader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalYamlDspConfigLoader extends AbstractYamlDspConfigLoader {
    @Override
    InputStream openStream(String location) throws Exception {
        var path = Path.of(location);
        if (!Files.exists(path)) {
            throw new IllegalStateException("dsps.yml not found at " + path.toAbsolutePath());
        }
        return Files.newInputStream(path);
    }

    @Override
    public long lastModified(String location) {
        try {
            var path = Path.of(location);
            if (!Files.exists(path)) {
                throw new IllegalStateException("dsps.yml not found at " + path.toAbsolutePath());
            }
            return Files.getLastModifiedTime(path).toMillis();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read dsps.yml timestamp", ex);
        }
    }
}
