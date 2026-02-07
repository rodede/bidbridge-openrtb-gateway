package ro.dede.bidbridge.simulator.config.loader;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3YamlDspConfigLoaderTest {

    @Test
    void delegatesToInjectedS3Client() {
        var headCalls = new AtomicInteger();
        var capturedRequest = new AtomicReference<HeadObjectRequest>();
        var s3Client = (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("headObject".equals(method.getName())) {
                        headCalls.incrementAndGet();
                        capturedRequest.set((HeadObjectRequest) args[0]);
                        return HeadObjectResponse.builder()
                                .lastModified(Instant.ofEpochMilli(123L))
                                .build();
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "s3";
                    }
                    return null;
                }
        );

        var loader = new S3YamlDspConfigLoader(s3Client);
        var lastModified = loader.lastModified("s3://bucket-name/path/to/dsps.yml");

        assertEquals(123L, lastModified);
        assertEquals(1, headCalls.get());
        assertEquals("bucket-name", capturedRequest.get().bucket());
        assertEquals("path/to/dsps.yml", capturedRequest.get().key());
    }

    @Test
    void rejectsNonS3Scheme() {
        var s3Client = (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> null
        );
        var loader = new S3YamlDspConfigLoader(s3Client);

        assertThrows(IllegalStateException.class, () -> loader.lastModified("file:///tmp/dsps.yml"));
    }
}
