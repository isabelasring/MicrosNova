package co.com.bancolombia.visionadapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class VisionAdapterConfig {
    @Bean(destroyMethod = "close")
    public SnsClient snsClient(@Value("${vision.aws.region}") String region) {
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public VisionEnvironmentAdapter visionEnvironmentAdapter(
            @Value("${vision.environment:dev}") String environment
    ) {
        return new VisionEnvironmentAdapter(environment);
    }
}
