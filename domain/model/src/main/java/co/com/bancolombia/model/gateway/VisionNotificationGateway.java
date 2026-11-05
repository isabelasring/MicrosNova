package co.com.bancolombia.model.gateway;

import co.com.bancolombia.model.AlarmPayload;

public interface VisionNotificationGateway {
    void notifyIris(AlarmPayload payload);

    void notifyAlice(AlarmPayload payload);

    void notifyAiopsAgent(AlarmPayload payload);
}
