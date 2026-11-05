package co.com.bancolombia.visionadapter;

import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.gateway.VisionNotificationGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VisionRestNotificationAdapter implements VisionNotificationGateway {
    private final RestClient irisClient;
    private final RestClient aliceClient;
    private final RestClient aiopsClient;

    public VisionRestNotificationAdapter(
            RestClient.Builder builder,
            @Value("${vision.clients.iris-url}") String irisUrl,
            @Value("${vision.clients.alice-url}") String aliceUrl,
            @Value("${vision.clients.aiops-agent-url}") String aiopsAgentUrl
    ) {
        this.irisClient = builder.clone().baseUrl(irisUrl).build();
        this.aliceClient = builder.clone().baseUrl(aliceUrl).build();
        this.aiopsClient = builder.clone().baseUrl(aiopsAgentUrl).build();
    }

    @Override
    public void notifyIris(AlarmPayload payload) {
        post(irisClient, "/", payload);
    }

    @Override
    public void notifyAlice(AlarmPayload payload) {
        post(aliceClient, "", payload);
    }

    @Override
    public void notifyAiopsAgent(AlarmPayload payload) {
        post(aiopsClient, "/", payload);
    }

    private void post(RestClient client, String uri, AlarmPayload payload) {
        try {
            client.post()
                    .uri(uri)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Falló el envío de una notificación HTTP", exception);
        }
    }
}
