package co.com.bancolombia.model;

public record VisionAlert(
        String eventStatus,
        String externalUrl,
        String fingerprint,
        String appCode,
        String alertName,
        String cluster,
        String componentName,
        String componentId,
        String componentType,
        String severity,
        String startsAt,
        String endsAt,
        String description,
        String summary
) {
}
