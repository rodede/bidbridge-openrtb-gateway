package ro.dede.bidbridge.simulator.config.loader;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.InputStream;
import java.net.URI;

public final class S3YamlDspConfigLoader extends AbstractYamlDspConfigLoader {
    private final String region;

    public S3YamlDspConfigLoader(String region) {
        this.region = region;
    }

    @Override
    InputStream openStream(String location) {
        var s3Location = parse(location);
        return s3Client(s3Location.region).getObject(GetObjectRequest.builder()
                .bucket(s3Location.bucket)
                .key(s3Location.key)
                .build());
    }

    @Override
    public long lastModified(String location) {
        var s3Location = parse(location);
        var head = s3Client(s3Location.region).headObject(HeadObjectRequest.builder()
                .bucket(s3Location.bucket)
                .key(s3Location.key)
                .build());
        return head.lastModified().toEpochMilli();
    }

    private S3Location parse(String location) {
        var uri = URI.create(location);
        var bucket = uri.getHost();
        var key = uri.getPath();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Invalid s3 location: missing bucket");
        }
        if (key == null || key.isBlank() || "/".equals(key)) {
            throw new IllegalStateException("Invalid s3 location: missing key");
        }
        key = key.startsWith("/") ? key.substring(1) : key;
        return new S3Location(bucket, key, region);
    }

    private S3Client s3Client(String region) {
        if (region == null || region.isBlank()) {
            return S3Client.builder().build();
        }
        return S3Client.builder().region(Region.of(region)).build();
    }

    private record S3Location(String bucket, String key, String region) {
    }
}
