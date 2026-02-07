package ro.dede.bidbridge.simulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.dede.bidbridge.simulator.config.loader.S3YamlDspConfigLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class DspsS3Config {

    @Bean(destroyMethod = "close")
    S3Client dspsS3Client(DspsFileProperties properties) {
        var region = properties.getAwsRegion();
        if (region == null || region.isBlank()) {
            return S3Client.builder().build();
        }
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Bean
    S3YamlDspConfigLoader dspsS3YamlDspConfigLoader(S3Client dspsS3Client) {
        return new S3YamlDspConfigLoader(dspsS3Client);
    }
}
