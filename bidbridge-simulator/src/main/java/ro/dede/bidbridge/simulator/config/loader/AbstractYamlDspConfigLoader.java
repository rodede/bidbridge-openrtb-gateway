package ro.dede.bidbridge.simulator.config.loader;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import ro.dede.bidbridge.simulator.config.DspConfig;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractYamlDspConfigLoader implements DspConfigLoader {
    @Override
    public DspConfigLoadResult load(String location) {
        try (InputStream input = openStream(location)) {
            return loadFrom(input, lastModified(location));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load dsps.yml", ex);
        }
    }

    abstract InputStream openStream(String location) throws Exception;

    DspConfigLoadResult loadFrom(InputStream input, long versionTimestamp) {
        var options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        var yaml = new Yaml(options);
        var root = yaml.load(input);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalStateException("Invalid dsps.yml: expected a map at root");
        }
        Map<?, ?> dspsMap;
        var dspsNode = rootMap.get("dsps");
        if (dspsNode instanceof Map<?, ?> map) {
            dspsMap = map;
        } else {
            dspsMap = rootMap;
        }
        var configs = parseConfigs(dspsMap);
        validate(configs);
        return new DspConfigLoadResult(configs, versionTimestamp);
    }

    private Map<String, DspConfig> parseConfigs(Map<?, ?> dspsMap) {
        var configs = new LinkedHashMap<String, DspConfig>();
        for (var entry : dspsMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (!(key instanceof String name)) {
                throw new IllegalStateException("Invalid dsp name: " + key);
            }
            if (!(value instanceof Map<?, ?> configMap)) {
                throw new IllegalStateException("Invalid dsp config for " + name);
            }
            configs.put(name, toConfig(configMap));
        }
        return Collections.unmodifiableMap(configs);
    }

    private DspConfig toConfig(Map<?, ?> map) {
        var config = new DspConfig();
        setBoolean(map, "enabled", config::setEnabled);
        setDouble(map, "bidProbability", config::setBidProbability);
        setDouble(map, "fixedPrice", config::setFixedPrice);
        setString(map, "currency", config::setCurrency);
        setString(map, "admTemplate", config::setAdmTemplate);
        setInt(map, "responseDelayMs", config::setResponseDelayMs);
        return config;
    }

    private void validate(Map<String, DspConfig> configs) {
        for (var entry : configs.entrySet()) {
            var name = entry.getKey();
            var config = entry.getValue();
            if (config == null) {
                throw new IllegalStateException("Null config for dsp " + name);
            }
            if (config.getBidProbability() < 0 || config.getBidProbability() > 1) {
                throw new IllegalStateException("bidProbability out of range for dsp " + name);
            }
            if (config.getResponseDelayMs() < 0) {
                throw new IllegalStateException("responseDelayMs must be >= 0 for dsp " + name);
            }
        }
    }

    private void setBoolean(Map<?, ?> map, String key, java.util.function.Consumer<Boolean> setter) {
        if (map.containsKey(key)) {
            var value = map.get(key);
            if (value instanceof Boolean b) {
                setter.accept(b);
            } else {
                throw new IllegalStateException("Invalid boolean for " + key);
            }
        }
    }

    private void setDouble(Map<?, ?> map, String key, java.util.function.Consumer<Double> setter) {
        if (map.containsKey(key)) {
            var value = map.get(key);
            if (value instanceof Number n) {
                setter.accept(n.doubleValue());
            } else {
                throw new IllegalStateException("Invalid number for " + key);
            }
        }
    }

    private void setInt(Map<?, ?> map, String key, java.util.function.Consumer<Integer> setter) {
        if (map.containsKey(key)) {
            var value = map.get(key);
            if (value instanceof Number n) {
                setter.accept(n.intValue());
            } else {
                throw new IllegalStateException("Invalid number for " + key);
            }
        }
    }

    private void setString(Map<?, ?> map, String key, java.util.function.Consumer<String> setter) {
        if (map.containsKey(key)) {
            var value = map.get(key);
            if (value instanceof String s) {
                setter.accept(s);
            } else {
                throw new IllegalStateException("Invalid string for " + key);
            }
        }
    }
}
