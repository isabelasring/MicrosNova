package co.com.bancolombia.cloudwatchadapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class CloudWatchAdapterConfig {
    @Bean(destroyMethod = "close")
    public SnsClient snsClient(@Value("${cloudwatch.aws.region}") String region) {
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public CloudWatchEnvironmentAdapter cloudwatchEnvironmentAdapter(
            @Value("${cloudwatch.environment:dev}") String environment
    ) {
        return new CloudWatchEnvironmentAdapter(environment);
    }
}
