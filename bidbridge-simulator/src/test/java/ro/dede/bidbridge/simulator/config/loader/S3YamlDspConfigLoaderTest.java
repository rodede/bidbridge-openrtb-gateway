package ro.dede.bidbridge.simulator.config.loader;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class S3YamlDspConfigLoaderTest {

    @Test
    void reusesSingleS3ClientAcrossCallsAndClosesOnShutdown() {
        var createCount = new AtomicInteger();
        var closeCount = new AtomicInteger();
        var s3Client = (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        closeCount.incrementAndGet();
                        return null;
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "s3";
                    }
                    return null;
                }
        );

        var loader = new S3YamlDspConfigLoader(() -> {
            createCount.incrementAndGet();
            return s3Client;
        });

        var first = loader.s3Client();
        var second = loader.s3Client();

        assertSame(first, second);
        assertEquals(1, createCount.get());

        loader.close();
        assertEquals(1, closeCount.get());
    }
}
