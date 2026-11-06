package co.com.bancolombia.interpretevision;

import co.com.bancolombia.model.VisionAlert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record VisionWebhookRequest(
        @NotBlank
        String status,
        @NotBlank
        String externalURL,
        @NotEmpty
        List<@Valid Alert> alerts
) {
    public record Alert(
            @NotBlank
            String fingerprint,
            Map<String, String> labels,
            String startsAt,
            String endsAt,
            Map<String, String> annotations
    ) {
    }

    public List<VisionAlert> toVisionAlerts() {
        if (alerts == null || alerts.isEmpty()) {
            throw new IllegalArgumentException("El evento debe incluir al menos una alerta");
        }

        List<VisionAlert> translated = new ArrayList<>();
        for (Alert alert : alerts) {
            if (alert == null) {
                throw new IllegalArgumentException("El evento debe incluir al menos una alerta");
            }
            Map<String, String> labels = alert.labels() == null ? Map.of() : alert.labels();
            Map<String, String> annotations =
                    alert.annotations() == null ? Map.of() : alert.annotations();
            System.out.println("[VisionWebhookRequest] Abre una alerta. Estado: " + status
                    + ". Etiquetas: " + labels
                    + ". Anotaciones: " + annotations);
            translated.add(new VisionAlert(
                    status,
                    externalURL,
                    alert.fingerprint(),
                    labels.get("appCode"),
                    labels.get("alertname"),
                    labels.get("cluster"),
                    labels.get("componentName"),
                    labels.get("componentID"),
                    labels.get("componentType"),
                    labels.get("severity"),
                    alert.startsAt(),
                    alert.endsAt(),
                    annotations.get("description"),
                    annotations.get("summary")
            ));
        }
        return List.copyOf(translated);
    }
}
