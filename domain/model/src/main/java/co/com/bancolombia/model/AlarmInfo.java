package co.com.bancolombia.model;

public record AlarmInfo(
        String source,
        String name,
        String status,
        String env,
        String accountID,
        String date,
        String description,
        String id,
        String url,
        AffectedEntity affectedEntity
) {
}
