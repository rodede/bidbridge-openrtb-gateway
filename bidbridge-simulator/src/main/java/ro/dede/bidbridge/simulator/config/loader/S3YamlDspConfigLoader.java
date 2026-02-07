package ro.dede.bidbridge.simulator.config.loader;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

public final class S3YamlDspConfigLoader extends AbstractYamlDspConfigLoader implements AutoCloseable {
    private final Supplier<S3Client> s3ClientFactory;
    private volatile S3Client s3Client;

    public S3YamlDspConfigLoader(String region) {
        this(() -> createS3Client(region));
    }

    S3YamlDspConfigLoader(Supplier<S3Client> s3ClientFactory) {
        this.s3ClientFactory = s3ClientFactory;
    }

    @Override
    InputStream openStream(String location) {
        var s3Location = parse(location);
        return s3Client().getObject(GetObjectRequest.builder()
                .bucket(s3Location.bucket)
                .key(s3Location.key)
                .build());
    }

    @Override
    public long lastModified(String location) {
        var s3Location = parse(location);
        var head = s3Client().headObject(HeadObjectRequest.builder()
                .bucket(s3Location.bucket)
                .key(s3Location.key)
                .build());
        return head.lastModified().toEpochMilli();
    }

    private S3Location parse(String location) {
        var uri = URI.create(location);
        if (!"s3".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("Invalid s3 location: scheme must be s3");
        }
        var bucket = uri.getHost();
        var key = uri.getPath();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Invalid s3 location: missing bucket");
        }
        if (key == null || key.isBlank() || "/".equals(key)) {
            throw new IllegalStateException("Invalid s3 location: missing key");
        }
        key = key.startsWith("/") ? key.substring(1) : key;
        return new S3Location(bucket, key);
    }

    S3Client s3Client() {
        var current = s3Client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (s3Client == null) {
                s3Client = s3ClientFactory.get();
            }
            return s3Client;
        }
    }

    private static S3Client createS3Client(String region) {
        if (region == null || region.isBlank()) {
            return S3Client.builder().build();
        }
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public void close() {
        S3Client current;
        synchronized (this) {
            current = s3Client;
            s3Client = null;
        }
        if (current != null) {
            current.close();
        }
    }

    private record S3Location(String bucket, String key) {
    }
}
