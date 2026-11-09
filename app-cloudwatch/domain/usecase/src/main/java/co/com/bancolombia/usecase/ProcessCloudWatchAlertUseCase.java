package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AffectedEntity;
import co.com.bancolombia.model.AlarmDataException;
import co.com.bancolombia.model.AlarmInfo;
import co.com.bancolombia.model.AlarmMetric;
import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.ApplicationCodeNotFoundException;
import co.com.bancolombia.model.BackupAlarm;
import co.com.bancolombia.model.CloudWatchAlarm;
import co.com.bancolombia.model.CloudWatchAlarm.MetricItem;
import co.com.bancolombia.model.CloudWatchAlarm.Trigger;
import co.com.bancolombia.model.CloudWatchEvent;
import co.com.bancolombia.model.CloudWatchProcessResult;
import co.com.bancolombia.model.gateway.CloudWatchEnvironment;
import co.com.bancolombia.model.gateway.CloudWatchSnsGateway;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessCloudWatchAlertUseCase {
    private static final DateTimeFormatter ALARM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern APPLICATION_CODE = Pattern.compile("(\\w+\\d)");
    private static final String PROCESSED = "Procesado exitosamente";

    private final CloudWatchSnsGateway snsGateway;
    private final CloudWatchEnvironment environment;

    public ProcessCloudWatchAlertUseCase(
            CloudWatchSnsGateway snsGateway,
            CloudWatchEnvironment environment
    ) {
        this.snsGateway = snsGateway;
        this.environment = environment;
    }

    public CloudWatchProcessResult execute(CloudWatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Invalid SNS message format");
        }

        AlarmPayload payload = switch (event) {
            case CloudWatchAlarm alarm -> fromAlarm(alarm);
            case BackupAlarm backup -> fromBackup(backup);
        };
        System.out.println("[ProcessCloudWatchAlertUseCase] Publica " + payload.alarmInfo().name());
        snsGateway.publish(payload);
        System.out.println("[ProcessCloudWatchAlertUseCase] Devuelve la frase: " + PROCESSED);
        return new CloudWatchProcessResult(PROCESSED);
    }

    private AlarmPayload fromAlarm(CloudWatchAlarm alarm) {
        System.out.println("[ProcessCloudWatchAlertUseCase] Traduce la alarma de CloudWatch");
        List<String> missing = new ArrayList<>();
        if (alarm.alarmName() == null || alarm.alarmName().isBlank()) {
            missing.add("AlarmName");
        }
        if (alarm.accountId() == null || alarm.accountId().isBlank()) {
            missing.add("AWSAccountId");
        }
        if (alarm.stateChangeTime() == null || alarm.stateChangeTime().isBlank()) {
            missing.add("StateChangeTime");
        }
        if (!missing.isEmpty()) {
            throw new AlarmDataException("Missing required fields: " + String.join(", ", missing));
        }

        String alarmDate = toBogotaTime(alarm.stateChangeTime());
        String alarmEnv = environmentOf(alarm.alarmName());
        AlarmMetric metric = readMetric(alarm.trigger());
        AffectedEntity affectedEntity = CloudWatchAffectedEntityResolver.resolve(
                alarm.trigger(),
                alarm.alarmArn(),
                alarm.accountId()
        );
        String appCode = applicationCode(alarm.alarmName());
        if (alarm.alarmDescription() == null) {
            throw new AlarmDataException("Error processing event");
        }
        String description = "Hemos detectado un alertamiento en la aplicacion "
                + appCode + " con la siguiente descripcion: " + alarm.alarmDescription();

        return new AlarmPayload(new AlarmInfo(
                "cloudwatch",
                alarm.alarmName(),
                "ALARM".equals(alarm.newStateValue()) ? "open" : "closed",
                alarmEnv,
                alarm.accountId(),
                alarmDate,
                description,
                alarm.alarmArn(),
                "N/A",
                affectedEntity,
                metric
        ));
    }

    private AlarmPayload fromBackup(BackupAlarm backup) {
        System.out.println("[ProcessCloudWatchAlertUseCase] Traduce el evento de backup");
        if (backup.appCode() == null || backup.appCode().isBlank()) {
            System.out.println("[ProcessCloudWatchAlertUseCase] No se logró obtener el appCode del evento de backup con alarmID "
                    + backup.alarmId());
            throw new ApplicationCodeNotFoundException(
                    "No se logró obtener el appCode del evento de backup con alarmID"
            );
        }
        String alarmDate = toBogotaTime(backup.alarmDate());
        return new AlarmPayload(new AlarmInfo(
                "Backups",
                backup.alarmName(),
                backup.alarmStatus(),
                backup.alarmEnv(),
                backup.alarmAccountId(),
                alarmDate,
                backup.alarmDescription(),
                backup.alarmId(),
                backup.alarmUrl(),
                new AffectedEntity(backup.entityName(), backup.entityId(), backup.entityType()),
                null
        ));
    }

    private String environmentOf(String alarmName) {
        String env = environment.currentEnvironment();
        if (env != null && alarmName.toLowerCase(Locale.ROOT).contains(env.toLowerCase(Locale.ROOT))) {
            return env.toLowerCase(Locale.ROOT);
        }
        System.out.println("[ProcessCloudWatchAlertUseCase] Alarma en ambiente incorrecto");
        throw new IllegalArgumentException("Alarma en ambiente incorrecto");
    }

    private String applicationCode(String alarmName) {
        Matcher matcher = APPLICATION_CODE.matcher(alarmName.replace('_', '-'));
        if (!matcher.find()) {
            System.out.println("[ProcessCloudWatchAlertUseCase] No se continua con el procesamiento de la alarma. "
                    + "Falta codigo de aplicacion. Revisar: " + alarmName);
            throw new ApplicationCodeNotFoundException(
                    "No se pudo obtener el codigo de aplicacion del nombre de la alarma"
            );
        }
        return matcher.group().toLowerCase(Locale.ROOT);
    }

    private AlarmMetric readMetric(Trigger trigger) {
        String name = "";
        String operator = "";
        double threshold = 0;
        int period = 0;
        try {
            if (trigger == null) {
                throw new IllegalStateException("sin trigger");
            }
            if (trigger.metrics() != null) {
                List<MetricItem> metrics = trigger.metrics();
                String id = metrics.get(metrics.size() - 1).id();
                name = id == null ? "" : id;
            } else if (trigger.metricName() != null) {
                name = trigger.metricName();
            } else {
                throw new IllegalStateException("sin nombre de metrica");
            }
            if (trigger.comparisonOperator() == null) {
                throw new IllegalStateException("sin operador");
            }
            operator = trigger.comparisonOperator();
            if (trigger.threshold() == null) {
                throw new IllegalStateException("sin umbral");
            }
            threshold = trigger.threshold();
            if (trigger.period() == null) {
                throw new IllegalStateException("sin periodo");
            }
            period = trigger.period();
        } catch (RuntimeException exception) {
            System.out.println("[ProcessCloudWatchAlertUseCase] No se logro obtener los valores de la metrica. "
                    + exception);
        }
        return new AlarmMetric(name, operator, threshold, period);
    }

    private String toBogotaTime(String timestamp) {
        try {
            String value = timestamp;
            if (value.matches(".*[+-]\\d{4}$")) {
                int cut = value.length() - 2;
                value = value.substring(0, cut) + ":" + value.substring(cut);
            }
            return OffsetDateTime.parse(value)
                    .withOffsetSameInstant(ZoneOffset.ofHours(-5))
                    .format(ALARM_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new AlarmDataException("Error processing event");
        }
    }
}
