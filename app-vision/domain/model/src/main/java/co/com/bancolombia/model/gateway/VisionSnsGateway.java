package co.com.bancolombia.model.gateway;

import co.com.bancolombia.model.AlarmPayload;

public interface VisionSnsGateway {
    void publish(AlarmPayload payload);
}
