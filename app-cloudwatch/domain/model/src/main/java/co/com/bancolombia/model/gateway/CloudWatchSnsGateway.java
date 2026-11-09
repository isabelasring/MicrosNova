package co.com.bancolombia.model.gateway;

import co.com.bancolombia.model.AlarmPayload;

public interface CloudWatchSnsGateway {
    void publish(AlarmPayload payload);
}
