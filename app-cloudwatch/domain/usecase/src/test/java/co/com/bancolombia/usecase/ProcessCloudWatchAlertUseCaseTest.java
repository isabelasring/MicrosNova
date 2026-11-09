package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.ApplicationCodeNotFoundException;
import co.com.bancolombia.model.BackupAlarm;
import co.com.bancolombia.model.CloudWatchAlarm;
import co.com.bancolombia.model.CloudWatchAlarm.Dimension;
import co.com.bancolombia.model.CloudWatchAlarm.Trigger;
import co.com.bancolombia.model.CloudWatchProcessResult;
import co.com.bancolombia.model.AlarmDataException;
import co.com.bancolombia.model.gateway.CloudWatchEnvironment;
import co.com.bancolombia.model.gateway.CloudWatchSnsGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCloudWatchAlertUseCaseTest {
    private CloudWatchSnsGateway snsGateway;
    private CloudWatchEnvironment environment;
    private ProcessCloudWatchAlertUseCase useCase;

    @BeforeEach
    void setUp() {
        snsGateway = mock(CloudWatchSnsGateway.class);
        environment = mock(CloudWatchEnvironment.class);
        when(environment.currentEnvironment()).thenReturn("dev");
        useCase = new ProcessCloudWatchAlertUseCase(snsGateway, environment);
    }

    @Test
    void publishesAnOpenCloudWatchAlarmToTheIngestionMailbox() {
        CloudWatchProcessResult result = useCase.execute(new CloudWatchAlarm(
                "arn:aws:cloudwatch:us-east-1:123456789012:alarm:dev_app1_alarm",
                "dev_app1_alarm",
                "123456789012",
                "2026-09-21T10:00:00.000+0000",
                "ALARM",
                "CPU alta",
                new Trigger(
                        "CPUUtilization",
                        "GreaterThanThreshold",
                        80.0,
                        60,
                        "AWS/EC2",
                        List.of(new Dimension("InstanceId", "i-abc")),
                        null,
                        null
                )
        ));

        ArgumentCaptor<AlarmPayload> published = ArgumentCaptor.forClass(AlarmPayload.class);
        verify(snsGateway).publish(published.capture());
        var alarmInfo = published.getValue().alarmInfo();
        assertEquals("Procesado exitosamente", result.message());
        assertEquals("cloudwatch", alarmInfo.source());
        assertEquals("open", alarmInfo.status());
        assertEquals("dev", alarmInfo.env());
        assertEquals("2026-09-21 05:00:00", alarmInfo.date());
        assertEquals("arn:aws:cloudwatch:us-east-1:123456789012:alarm:dev_app1_alarm", alarmInfo.id());
        assertEquals("N/A", alarmInfo.url());
        assertTrue(alarmInfo.description().contains("app1"));
        assertTrue(alarmInfo.description().contains("CPU alta"));
        assertEquals("i-abc", alarmInfo.affectedEntity().entityName());
        assertEquals("arn:aws:ec2:us-east-1:123456789012:instance/i-abc", alarmInfo.affectedEntity().entityId());
        assertEquals("EC2", alarmInfo.affectedEntity().entityType());
        assertEquals("CPUUtilization", alarmInfo.metric().name());
        assertEquals(80.0, alarmInfo.metric().threshold());
    }

    @Test
    void closesTheAlarmWhenCloudWatchIsNotInAlarm() {
        useCase.execute(alarm("OK", "dev_app1_alarm"));

        ArgumentCaptor<AlarmPayload> published = ArgumentCaptor.forClass(AlarmPayload.class);
        verify(snsGateway).publish(published.capture());
        assertEquals("closed", published.getValue().alarmInfo().status());
    }

    @Test
    void rejectsAnAlarmFromAnotherEnvironment() {
        when(environment.currentEnvironment()).thenReturn("qa");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(alarm("ALARM", "dev_app1_alarm"))
        );

        assertEquals("Alarma en ambiente incorrecto", error.getMessage());
        verify(snsGateway, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAnAlarmWithoutApplicationCode() {
        ApplicationCodeNotFoundException error = assertThrows(
                ApplicationCodeNotFoundException.class,
                () -> useCase.execute(alarm("ALARM", "dev_alarm_without_code"))
        );

        assertEquals("No se pudo obtener el codigo de aplicacion del nombre de la alarma", error.getMessage());
        verify(snsGateway, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAnAlarmWithoutRequiredFields() {
        AlarmDataException error = assertThrows(
                AlarmDataException.class,
                () -> useCase.execute(new CloudWatchAlarm(
                        "arn:aws:cloudwatch:us-east-1:123:alarm:x",
                        " ",
                        null,
                        "2026-09-21T10:00:00.000+0000",
                        "ALARM",
                        "desc",
                        null
                ))
        );

        assertEquals("Missing required fields: AlarmName, AWSAccountId", error.getMessage());
    }

    @Test
    void publishesABackupWithoutCallingOtherServices() {
        CloudWatchProcessResult result = useCase.execute(new BackupAlarm(
                "backup-1",
                "app1",
                "Backup failed",
                "open",
                "dev",
                "123456789012",
                "2026-09-21T10:00:00.000+0000",
                "Failure",
                "N/A",
                "backup",
                "backup-1",
                "Backup"
        ));

        ArgumentCaptor<AlarmPayload> published = ArgumentCaptor.forClass(AlarmPayload.class);
        verify(snsGateway).publish(published.capture());
        var alarmInfo = published.getValue().alarmInfo();
        assertEquals("Procesado exitosamente", result.message());
        assertEquals("Backups", alarmInfo.source());
        assertEquals("backup-1", alarmInfo.id());
        assertEquals("Failure", alarmInfo.description());
        assertEquals("backup", alarmInfo.affectedEntity().entityName());
        assertNull(alarmInfo.metric());
    }

    @Test
    void rejectsABackupWithoutApplicationCode() {
        ApplicationCodeNotFoundException error = assertThrows(
                ApplicationCodeNotFoundException.class,
                () -> useCase.execute(new BackupAlarm(
                        "backup-1",
                        " ",
                        "Backup failed",
                        "open",
                        "dev",
                        "123",
                        "2026-09-21T10:00:00.000+0000",
                        "Failure",
                        "N/A",
                        "backup",
                        "backup-1",
                        "Backup"
                ))
        );

        assertEquals("No se logró obtener el appCode del evento de backup con alarmID", error.getMessage());
    }

    private CloudWatchAlarm alarm(String state, String name) {
        return new CloudWatchAlarm(
                "arn:aws:cloudwatch:us-east-1:123456789012:alarm:" + name,
                name,
                "123456789012",
                "2026-09-21T10:00:00.000+0000",
                state,
                "CPU alta",
                new Trigger("CPUUtilization", "GreaterThanThreshold", 80.0, 60, "AWS/EC2",
                        List.of(new Dimension("InstanceId", "i-abc")), null, null)
        );
    }
}
