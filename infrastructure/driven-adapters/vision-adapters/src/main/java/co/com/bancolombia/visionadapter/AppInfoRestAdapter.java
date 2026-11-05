package co.com.bancolombia.visionadapter;

import co.com.bancolombia.model.ApplicationInfo;
import co.com.bancolombia.model.gateway.ApplicationInfoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class AppInfoRestAdapter implements ApplicationInfoGateway {
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public AppInfoRestAdapter(
            RestClient.Builder builder,
            @Value("${vision.clients.app-info-url}") String appInfoUrl
    ) {
        this.restClient = builder.baseUrl(appInfoUrl).build();
    }

    @Override
    public ApplicationInfo findByAppCodeAndDate(String appCode, String alarmDate) {
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/")
                    .body(Map.of("appcode", appCode, "alarmdate", alarmDate))
                    .retrieve()
                    .body(RESPONSE_TYPE);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Falló la consulta a ms-appinfo", exception);
        }

        if (response == null) {
            throw new IllegalStateException("ms-appinfo devolvió una respuesta vacía");
        }
        return new ApplicationInfo(response);
    }
}
