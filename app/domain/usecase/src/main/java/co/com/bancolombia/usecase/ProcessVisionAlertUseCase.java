package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AffectedEntity;
import co.com.bancolombia.model.AlarmInfo;
import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.VisionAlert;
import co.com.bancolombia.model.VisionProcessResult;
import co.com.bancolombia.model.gateway.VisionEnvironment;
import co.com.bancolombia.model.gateway.VisionSnsGateway;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProcessVisionAlertUseCase {
    private static final DateTimeFormatter ALARM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROCESSED =
            "Evento procesado satisfactoriamente";

    private final VisionSnsGateway snsGateway;
    private final VisionEnvironment environment;

    public ProcessVisionAlertUseCase(
            VisionSnsGateway snsGateway,
            VisionEnvironment environment
    ) {
        this.snsGateway = snsGateway;
        this.environment = environment;
    }

    public VisionProcessResult execute(List<VisionAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            throw new IllegalArgumentException("El evento debe incluir al menos una alerta");
        }

        System.out.println("[ProcessVisionAlertUseCase] Traduce y publica " + alerts.size() + " alerta(s)");
        for (VisionAlert alert : alerts) {
            validate(alert);
            AlarmPayload payload = buildPayload(alert);
            System.out.println("[ProcessVisionAlertUseCase] Publica " + alert.alertName());
            snsGateway.publish(payload);
        }

        System.out.println("[ProcessVisionAlertUseCase] Devuelve la frase: " + PROCESSED);
        return new VisionProcessResult(PROCESSED);
    }

    private AlarmPayload buildPayload(VisionAlert alert) {
        boolean firing = "firing".equalsIgnoreCase(alert.eventStatus());
        String status = firing ? "open" : "closed";
        String alarmDate = toBogotaTime(firing ? alert.startsAt() : alert.endsAt());
        String annotation = alert.description() != null ? alert.description() : alert.summary();
        String description = "Hemos detectado un alertamiento en la aplicacion "
                + alert.appCode() + " con la siguiente descripcion: " + annotation;
        String url = alert.externalUrl().replace(
                "/alertmanager",
                "/grafana/alerting/list?view=state"
        );

        AlarmInfo alarmInfo = new AlarmInfo(
                "vision",
                alert.alertName(),
                status,
                environment.currentEnvironment(),
                alert.cluster(),
                alarmDate,
                description,
                "vision_" + alert.fingerprint(),
                url,
                new AffectedEntity(
                        alert.componentName(),
                        alert.componentId(),
                        alert.componentType().toLowerCase(Locale.ROOT)
                )
        );

        return new AlarmPayload(alarmInfo);
    }

    private String toBogotaTime(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp)
                    .withOffsetSameInstant(ZoneOffset.ofHours(-5))
                    .format(ALARM_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("La fecha de la alerta no tiene un formato válido", exception);
        }
    }

    private void validate(VisionAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("El evento de Vision es obligatorio");
        }

        requireText(alert.eventStatus(), "status");
        requireText(alert.externalUrl(), "externalURL");
        requireText(alert.fingerprint(), "alerts.fingerprint");
        requireText(alert.appCode(), "alerts.labels.appCode");
        requireText(alert.alertName(), "alerts.labels.alertname");
        requireText(alert.cluster(), "alerts.labels.cluster");
        requireText(alert.componentName(), "alerts.labels.componentName");
        requireText(alert.componentId(), "alerts.labels.componentID");
        requireText(alert.componentType(), "alerts.labels.componentType");
        requireText(alert.severity(), "alerts.labels.severity");
        if (alert.description() == null && alert.summary() == null) {
            throw new IllegalArgumentException(
                    "Falta la descripción y el resumen de la alerta"
            );
        }
        requireText(
                "firing".equalsIgnoreCase(alert.eventStatus()) ? alert.startsAt() : alert.endsAt(),
                "alerts.startsAt/endsAt"
        );
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio: " + fieldName);
        }
    }
}
