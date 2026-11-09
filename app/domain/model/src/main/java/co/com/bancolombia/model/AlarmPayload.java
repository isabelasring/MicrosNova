package co.com.bancolombia.model;

public record AlarmPayload(AlarmInfo alarmInfo) {
    public AlarmPayload {
        System.out.println("[AlarmPayload] Mensaje traducido: " + alarmInfo);
    }
}
