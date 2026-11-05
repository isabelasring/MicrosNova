package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AffectedEntity;
import co.com.bancolombia.model.AlarmInfo;
import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.ApplicationInfo;
import co.com.bancolombia.model.VisionAlert;
import co.com.bancolombia.model.VisionProcessResult;
import co.com.bancolombia.model.gateway.ApplicationInfoGateway;
import co.com.bancolombia.model.gateway.VisionEnvironment;
import co.com.bancolombia.model.gateway.VisionNotificationGateway;
import co.com.bancolombia.model.gateway.VisionSnsGateway;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProcessVisionAlertUseCase {
    private static final DateTimeFormatter ALARM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String OUT_OF_SCHEDULE =
            "Evento fuera de horario de alertamiento";
    private static final String PROCESSED =
            "Evento procesado satisfactoriamente";

    private final ApplicationInfoGateway applicationInfoGateway;
    private final VisionNotificationGateway notificationGateway;
    private final VisionSnsGateway snsGateway;
    private final VisionEnvironment environment;

    public ProcessVisionAlertUseCase(
            ApplicationInfoGateway applicationInfoGateway,
            VisionNotificationGateway notificationGateway,
            VisionSnsGateway snsGateway,
            VisionEnvironment environment
    ) {
        this.applicationInfoGateway = applicationInfoGateway;
        this.notificationGateway = notificationGateway;
        this.snsGateway = snsGateway;
        this.environment = environment;
    }

    public VisionProcessResult execute(VisionAlert alert) {
        validate(alert);

        boolean firing = "firing".equalsIgnoreCase(alert.eventStatus());
        String status = firing ? "open" : "closed";
        String alarmDate = toBogotaTime(firing ? alert.startsAt() : alert.endsAt());
        ApplicationInfo applicationInfo =
                applicationInfoGateway.findByAppCodeAndDate(alert.appCode(), alarmDate);

        if (applicationInfo == null || applicationInfo.data().isEmpty()) {
            throw new IllegalStateException("ms-appinfo devolvió una respuesta vacía");
        }
        if (applicationInfo.name().isBlank()) {
            throw new IllegalStateException("ms-appinfo no devolvió el nombre de la aplicación");
        }

        AlarmPayload payload = buildPayload(alert, status, alarmDate, applicationInfo);
        routeNotifications(alert, status, applicationInfo, payload);

        return new VisionProcessResult(
                shouldNotify(alert, status, applicationInfo) ? PROCESSED : OUT_OF_SCHEDULE
        );
    }

    private AlarmPayload buildPayload(
            VisionAlert alert,
            String status,
            String alarmDate,
            ApplicationInfo applicationInfo
    ) {
        String annotation = alert.description() != null ? alert.description() : alert.summary();
        String description = "Hemos detectado un alertamiento en la aplicacion "
                + applicationInfo.name() + " con la siguiente descripcion: " + annotation;
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

        return new AlarmPayload(alarmInfo, List.of(applicationInfo.data()));
    }

    private void routeNotifications(
            VisionAlert alert,
            String status,
            ApplicationInfo applicationInfo,
            AlarmPayload payload
    ) {
        boolean onSchedule = applicationInfo.onSchedule();
        boolean critical = "critical".equalsIgnoreCase(alert.severity());
        String currentEnvironment = environment.currentEnvironment();

        if ("closed".equals(status)) {
            snsGateway.publish(payload);
            return;
        }

        if (!onSchedule) {
            return;
        }

        if (critical) {
            snsGateway.publish(payload);
            notificationGateway.notifyIris(payload);
            notificationGateway.notifyAlice(payload);
        } else {
            notificationGateway.notifyIris(payload);
        }

        if ("dev".equalsIgnoreCase(currentEnvironment)) {
            notificationGateway.notifyAiopsAgent(payload);
        }
    }

    private boolean shouldNotify(
            VisionAlert alert,
            String status,
            ApplicationInfo applicationInfo
    ) {
        return "closed".equals(status)
                || (applicationInfo.onSchedule() && "firing".equalsIgnoreCase(alert.eventStatus()));
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
        requireText(alert.fingerprint(), "alerts[0].fingerprint");
        requireText(alert.appCode(), "alerts[0].labels.appCode");
        requireText(alert.alertName(), "alerts[0].labels.alertname");
        requireText(alert.cluster(), "alerts[0].labels.cluster");
        requireText(alert.componentName(), "alerts[0].labels.componentName");
        requireText(alert.componentId(), "alerts[0].labels.componentID");
        requireText(alert.componentType(), "alerts[0].labels.componentType");
        requireText(alert.severity(), "alerts[0].labels.severity");
        if (alert.description() == null && alert.summary() == null) {
            throw new IllegalArgumentException(
                    "Falta la descripción y el resumen de la alerta"
            );
        }
        requireText(
                "firing".equalsIgnoreCase(alert.eventStatus()) ? alert.startsAt() : alert.endsAt(),
                "alerts[0].startsAt/endsAt"
        );
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio: " + fieldName);
        }
    }
}
