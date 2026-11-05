package co.com.bancolombia.visionadapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class VisionAdapterConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory);
    }

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
