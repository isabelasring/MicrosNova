package co.com.bancolombia.model;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

public record ApplicationInfo(Map<String, Object> data) {
    public ApplicationInfo {
        data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public String name() {
        Object value = data.get("name");
        return value instanceof String name ? name : "";
    }

    public boolean onSchedule() {
        Object notification = data.get("notification");
        if (!(notification instanceof Map<?, ?> notificationData)) {
            throw new IllegalStateException("ms-appinfo no devolvió la configuración de notificación");
        }

        Object onSchedule = notificationData.get("onSchedule");
        if (!(onSchedule instanceof Boolean scheduled)) {
            throw new IllegalStateException("ms-appinfo no devolvió notification.onSchedule");
        }
        return scheduled;
    }
}
