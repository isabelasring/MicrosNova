package co.com.bancolombia.visionadapter;

import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.gateway.VisionSnsGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
public class VisionSnsAdapter implements VisionSnsGateway {
    private final SnsClient snsClient;
    private final String topicArn;
    private final ObjectMapper objectMapper;

    public VisionSnsAdapter(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            @Value("${vision.aws.sns-topic-arn:}") String topicArn
    ) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AlarmPayload payload) {
        if (topicArn.isBlank()) {
            throw new IllegalStateException("No está configurado VISION_SNS_TOPIC_ARN");
        }

        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("No fue posible serializar la alerta para SNS", exception);
        }

        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .messageGroupId("cloudwatch")
                    .build());
        } catch (SnsException exception) {
            throw new IllegalStateException("Falló la publicación de la alerta en SNS", exception);
        }
    }
}
