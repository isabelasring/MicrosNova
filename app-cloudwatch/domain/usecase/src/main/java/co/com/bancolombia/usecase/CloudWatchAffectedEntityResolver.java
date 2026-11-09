package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AffectedEntity;
import co.com.bancolombia.model.AlarmDataException;
import co.com.bancolombia.model.CloudWatchAlarm.Dimension;
import co.com.bancolombia.model.CloudWatchAlarm.MetricItem;
import co.com.bancolombia.model.CloudWatchAlarm.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CloudWatchAffectedEntityResolver {
    private static final Pattern SCHEMA_ESCAPED = Pattern.compile(
            "SCHEMA\\(\\s*\\\\\"([^\\\\\"]+)\\\\\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SCHEMA_PLAIN = Pattern.compile(
            "SCHEMA\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WHERE_CLAUSE = Pattern.compile(
            "WHERE\\s+(.+?)(?:GROUP BY|ORDER BY|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DIMENSION_EQUALS = Pattern.compile("(\\w+)\\s*=\\s*'([^']*)'");

    private static final List<ArnRule> RULES = List.of(
            new ArnRule("AWS/EC2", "InstanceId", "arn:aws:ec2:%s:%s:instance/%s", true),
            new ArnRule("AWS/EC2", "AutoScalingGroupName", "arn:aws:autoscaling:%s:%s:autoScalingGroup:*:autoScalingGroupName/%s", true),
            new ArnRule("AWS/DynamoDB", "TableName", "arn:aws:dynamodb:%s:%s:table/%s", true),
            new ArnRule("AWS/Lambda", "FunctionName", "arn:aws:lambda:%s:%s:function:%s", true),
            new ArnRule("AWS/SNS", "TopicName", "arn:aws:sns:%s:%s:%s", true),
            new ArnRule("AWS/SQS", "QueueName", "arn:aws:sqs:%s:%s:%s", true),
            new ArnRule("AWS/RDS", "DBInstanceIdentifier", "arn:aws:rds:%s:%s:db:%s", true),
            new ArnRule("AWS/CloudFront", "DistributionId", "arn:aws:cloudfront::%s:distribution/%s", false),
            new ArnRule("AWS/ApplicationELB", "LoadBalancer", "arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s", true),
            new ArnRule("AWS/ELB", "LoadBalancerName", "arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s", true),
            new ArnRule("AWS/Events", "RuleName", "arn:aws:events:%s:%s:rule/%s", true),
            new ArnRule("AWS/States", "StateMachineArn", null, true),
            new ArnRule("AWS/ElastiCache", "CacheClusterId", "arn:aws:elasticache:%s:%s:cluster:%s", true),
            new ArnRule("AWS/NetworkELB", "LoadBalancer", "arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s", true),
            new ArnRule("AWS/CertificateManager", "CertificateArn", null, true),
            new ArnRule("AWS/AmazonMQ", "Broker", "arn:aws:mq:%s:%s:broker:%s", true),
            new ArnRule("CWAgent", "InstanceId", "arn:aws:ec2:%s:%s:instance/%s", true),
            new ArnRule("CWAgent", "AutoScalingGroupName", "arn:aws:autoscaling:%s:%s:autoScalingGroup:*:autoScalingGroupName/%s", true),
            new ArnRule("ContainerInsights", "ClusterName", "arn:aws:eks:%s:%s:cluster/%s", true)
    );

    private CloudWatchAffectedEntityResolver() {
    }

    static AffectedEntity resolve(Trigger trigger, String alarmArn, String accountId) {
        if (trigger == null) {
            throw new AlarmDataException("Error processing event");
        }
        String[] arnParts = alarmArn == null ? new String[0] : alarmArn.split(":", -1);
        if (arnParts.length <= 3) {
            throw new AlarmDataException("Error processing event");
        }
        String region = arnParts[3];
        NamespaceDimensions located = locate(trigger);
        String namespace = located.namespace();
        String entityType = namespace.contains("/") ? namespace.split("/")[1] : namespace;
        String[] identity = identityOf(namespace, located.dimensions(), region, accountId);
        return new AffectedEntity(identity[1], identity[0], entityType);
    }

    private static NamespaceDimensions locate(Trigger trigger) {
        if (trigger.namespace() != null) {
            List<Dimension> dimensions = trigger.dimensions() == null ? List.of() : trigger.dimensions();
            return new NamespaceDimensions(trigger.namespace(), dimensions);
        }
        if (trigger.metrics() != null) {
            if (trigger.metrics().isEmpty()) {
                throw new AlarmDataException("Error processing event");
            }
            MetricItem metric = trigger.metrics().get(0);
            if (metric.expression() != null) {
                return new NamespaceDimensions(
                        namespaceFromExpression(metric.expression()),
                        dimensionsFromExpression(metric.expression())
                );
            }
            String namespace = metric.namespace() == null ? "N/A" : metric.namespace();
            List<Dimension> dimensions = metric.dimensions() == null ? List.of() : metric.dimensions();
            return new NamespaceDimensions(namespace, dimensions);
        }
        if (trigger.metricExpression() != null) {
            return new NamespaceDimensions(
                    namespaceFromExpression(trigger.metricExpression()),
                    dimensionsFromExpression(trigger.metricExpression())
            );
        }
        return new NamespaceDimensions("N/A", List.of());
    }

    private static String[] identityOf(
            String namespace,
            List<Dimension> dimensions,
            String region,
            String accountId
    ) {
        try {
            Map<String, String> values = new LinkedHashMap<>();
            for (Dimension dimension : dimensions) {
                if (dimension != null && dimension.name() != null) {
                    values.put(dimension.name(), dimension.value());
                }
            }
            for (ArnRule rule : RULES) {
                if (!rule.namespace().equals(namespace) || !values.containsKey(rule.dimension())) {
                    continue;
                }
                String value = values.get(rule.dimension());
                if (rule.template() != null) {
                    String arn = rule.usesRegion()
                            ? rule.template().formatted(region, accountId, value)
                            : rule.template().formatted(accountId, value);
                    return new String[]{arn, value};
                }
                if ("StateMachineArn".equals(rule.dimension()) || "CertificateArn".equals(rule.dimension())) {
                    return new String[]{value, shortName(value)};
                }
            }
            System.out.println("[ProcessCloudWatchAlertUseCase] No se pudo determinar el ARN para namespace: " + namespace);
            return new String[]{"N/A", "N/A"};
        } catch (RuntimeException exception) {
            System.out.println("[ProcessCloudWatchAlertUseCase] Error determinando ARN para namespace "
                    + namespace + ": " + exception);
            return new String[]{"N/A", "N/A"};
        }
    }

    private static String shortName(String value) {
        if (value != null && value.contains(":")) {
            String[] parts = value.split(":");
            return parts[parts.length - 1];
        }
        if (value == null) {
            return "N/A";
        }
        String[] parts = value.split("/");
        return parts[parts.length - 1];
    }

    static String namespaceFromExpression(String expression) {
        Matcher escaped = SCHEMA_ESCAPED.matcher(expression);
        if (escaped.find()) {
            return escaped.group(1);
        }
        Matcher plain = SCHEMA_PLAIN.matcher(expression);
        return plain.find() ? plain.group(1) : "N/A";
    }

    static List<Dimension> dimensionsFromExpression(String expression) {
        List<Dimension> dimensions = new ArrayList<>();
        Matcher where = WHERE_CLAUSE.matcher(expression);
        if (!where.find()) {
            return dimensions;
        }
        Matcher equals = DIMENSION_EQUALS.matcher(where.group(1));
        while (equals.find()) {
            dimensions.add(new Dimension(equals.group(1), trimBackslashes(equals.group(2))));
        }
        return dimensions;
    }

    private static String trimBackslashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '\\') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '\\') {
            end--;
        }
        return value.substring(start, end);
    }

    private record ArnRule(String namespace, String dimension, String template, boolean usesRegion) {
    }

    private record NamespaceDimensions(String namespace, List<Dimension> dimensions) {
    }
}
