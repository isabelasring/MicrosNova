package co.com.bancolombia.model;

public record AlarmMetric(
        String name,
        String comparisonOperator,
        double threshold,
        int period
) {
}
