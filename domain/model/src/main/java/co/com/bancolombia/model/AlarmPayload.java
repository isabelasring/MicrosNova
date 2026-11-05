package co.com.bancolombia.model;

import java.util.List;
import java.util.Map;

public record AlarmPayload(AlarmInfo alarmInfo, List<Map<String, Object>> applicationInfo) {
}
