package co.com.bancolombia.model;

public record BackupAlarm(
        String alarmId,
        String appCode,
        String alarmName,
        String alarmStatus,
        String alarmEnv,
        String alarmAccountId,
        String alarmDate,
        String alarmDescription,
        String alarmUrl,
        String entityName,
        String entityId,
        String entityType
) implements CloudWatchEvent {
    public BackupAlarm {
        System.out.println("[BackupAlarm] Llega el backup: " + alarmName
                + ", aplicación: " + appCode
                + ", id: " + alarmId);
    }
}
