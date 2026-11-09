package co.com.bancolombia.interpretecloudwatch;

import co.com.bancolombia.model.BackupAlarm;
import co.com.bancolombia.model.CloudWatchAlarm;
import co.com.bancolombia.model.CloudWatchAlarm.Dimension;
import co.com.bancolombia.model.CloudWatchAlarm.MetricItem;
import co.com.bancolombia.model.CloudWatchAlarm.Trigger;
import co.com.bancolombia.model.CloudWatchEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CloudWatchMessageReader {
    private static final TypeReference<Map<String, Object>> MESSAGE = new TypeReference<>() {
    };

    private CloudWatchMessageReader() {
    }

    static CloudWatchEvent read(Object rawMessage, ObjectMapper objectMapper) {
        if (!(rawMessage instanceof String raw) || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid SNS message format");
        }
        Map<String, Object> message;
        try {
            message = objectMapper.readValue(raw, MESSAGE);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid SNS message format");
        }
        if (message == null) {
            throw new IllegalArgumentException("Invalid SNS message format");
        }
        System.out.println("[CloudWatchMessageReader] Abre el mensaje: " + message);
        if (message.containsKey("AlarmArn")) {
            return alarm(message);
        }
        return backup(message);
    }

    private static CloudWatchAlarm alarm(Map<String, Object> message) {
        return new CloudWatchAlarm(
                text(message.get("AlarmArn")),
                text(message.get("AlarmName")),
                text(message.get("AWSAccountId")),
                text(message.get("StateChangeTime")),
                text(message.get("NewStateValue")),
                text(message.get("AlarmDescription")),
                trigger(message.get("Trigger"))
        );
    }

    private static BackupAlarm backup(Map<String, Object> message) {
        return new BackupAlarm(
                text(message.get("alarmID")),
                text(message.get("appCode")),
                text(message.get("alarmName")),
                text(message.get("alarmStatus")),
                text(message.get("alarmEnv")),
                text(message.get("alarmAccountID")),
                text(message.get("alarmDate")),
                text(message.get("alarmDescription")),
                text(message.get("alarmURL")),
                text(message.get("entityName")),
                text(message.get("entityId")),
                text(message.get("entityType"))
        );
    }

    @SuppressWarnings("unchecked")
    private static Trigger trigger(Object rawTrigger) {
        if (!(rawTrigger instanceof Map<?, ?> trigger)) {
            return null;
        }
        Map<String, Object> values = (Map<String, Object>) trigger;
        return new Trigger(
                text(values.get("MetricName")),
                text(values.get("ComparisonOperator")),
                decimal(values.get("Threshold")),
                whole(values.get("Period")),
                values.containsKey("Namespace") ? text(values.get("Namespace")) : null,
                dimensions(values.get("Dimensions")),
                values.containsKey("Metrics") ? metrics(values.get("Metrics")) : null,
                text(values.get("MetricExpression"))
        );
    }

    @SuppressWarnings("unchecked")
    private static List<MetricItem> metrics(Object rawMetrics) {
        if (!(rawMetrics instanceof List<?> metrics)) {
            return List.of();
        }
        List<MetricItem> items = new ArrayList<>();
        for (Object rawMetric : metrics) {
            if (!(rawMetric instanceof Map<?, ?> metric)) {
                continue;
            }
            Map<String, Object> values = (Map<String, Object>) metric;
            Map<String, Object> metricObject = Map.of();
            Object rawStat = values.get("MetricStat");
            if (rawStat instanceof Map<?, ?> stat) {
                Object rawMetricObject = ((Map<String, Object>) stat).get("Metric");
                if (rawMetricObject instanceof Map<?, ?> nested) {
                    metricObject = (Map<String, Object>) nested;
                }
            }
            items.add(new MetricItem(
                    text(values.get("Id")),
                    text(values.get("Expression")),
                    metricObject.containsKey("Namespace") ? text(metricObject.get("Namespace")) : null,
                    dimensions(metricObject.get("Dimensions"))
            ));
        }
        return List.copyOf(items);
    }

    @SuppressWarnings("unchecked")
    private static List<Dimension> dimensions(Object rawDimensions) {
        if (!(rawDimensions instanceof List<?> dimensions)) {
            return List.of();
        }
        List<Dimension> items = new ArrayList<>();
        for (Object rawDimension : dimensions) {
            if (!(rawDimension instanceof Map<?, ?> dimension)) {
                continue;
            }
            Map<String, Object> values = (Map<String, Object>) dimension;
            items.add(new Dimension(text(values.get("name")), text(values.get("value"))));
        }
        return List.copyOf(items);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double decimal(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Integer whole(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
