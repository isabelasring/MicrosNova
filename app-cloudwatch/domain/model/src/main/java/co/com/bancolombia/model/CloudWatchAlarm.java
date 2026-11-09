package co.com.bancolombia.model;

import java.util.List;

public record CloudWatchAlarm(
        String alarmArn,
        String alarmName,
        String accountId,
        String stateChangeTime,
        String newStateValue,
        String alarmDescription,
        Trigger trigger
) implements CloudWatchEvent {
    public CloudWatchAlarm {
        System.out.println("[CloudWatchAlarm] Llega la alarma: " + alarmName
                + ", estado: " + newStateValue
                + ", cuenta: " + accountId);
    }

    public record Trigger(
            String metricName,
            String comparisonOperator,
            Double threshold,
            Integer period,
            String namespace,
            List<Dimension> dimensions,
            List<MetricItem> metrics,
            String metricExpression
    ) {
    }

    public record MetricItem(
            String id,
            String expression,
            String namespace,
            List<Dimension> dimensions
    ) {
    }

    public record Dimension(String name, String value) {
    }
}
