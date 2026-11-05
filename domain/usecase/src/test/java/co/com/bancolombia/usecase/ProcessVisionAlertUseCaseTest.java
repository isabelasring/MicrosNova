package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.ApplicationInfo;
import co.com.bancolombia.model.VisionAlert;
import co.com.bancolombia.model.VisionProcessResult;
import co.com.bancolombia.model.gateway.ApplicationInfoGateway;
import co.com.bancolombia.model.gateway.VisionEnvironment;
import co.com.bancolombia.model.gateway.VisionNotificationGateway;
import co.com.bancolombia.model.gateway.VisionSnsGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVisionAlertUseCaseTest {
    private ApplicationInfoGateway applicationInfoGateway;
    private VisionNotificationGateway notificationGateway;
    private VisionSnsGateway snsGateway;
    private VisionEnvironment environment;
    private ProcessVisionAlertUseCase useCase;

    @BeforeEach
    void setUp() {
        applicationInfoGateway = mock(ApplicationInfoGateway.class);
        notificationGateway = mock(VisionNotificationGateway.class);
        snsGateway = mock(VisionSnsGateway.class);
        environment = mock(VisionEnvironment.class);
        useCase = new ProcessVisionAlertUseCase(
                applicationInfoGateway,
                notificationGateway,
                snsGateway,
                environment
        );
        when(applicationInfoGateway.findByAppCodeAndDate("APP1", "2024-07-25 10:00:00"))
                .thenReturn(applicationInfo(true));
    }

    @Test
    void sendsCriticalFiringAlertToAllDevelopmentDestinations() {
        when(environment.currentEnvironment()).thenReturn("dev");

        VisionProcessResult result = useCase.execute(alert("firing", "critical"));

        assertEquals("Evento procesado satisfactoriamente", result.message());
        verify(snsGateway).publish(any(AlarmPayload.class));
        verify(notificationGateway).notifyIris(any(AlarmPayload.class));
        verify(notificationGateway).notifyAlice(any(AlarmPayload.class));
        verify(notificationGateway).notifyAiopsAgent(any(AlarmPayload.class));
    }

    @Test
    void sendsNonCriticalFiringAlertOnlyToIrisOutsideDevelopment() {
        when(environment.currentEnvironment()).thenReturn("qa");

        useCase.execute(alert("firing", "warning"));

        verify(snsGateway, never()).publish(any(AlarmPayload.class));
        verify(notificationGateway).notifyIris(any(AlarmPayload.class));
        verify(notificationGateway, never()).notifyAlice(any(AlarmPayload.class));
        verify(notificationGateway, never()).notifyAiopsAgent(any(AlarmPayload.class));
    }

    @Test
    void sendsResolvedAlertToSnsEvenOutsideSchedule() {
        when(environment.currentEnvironment()).thenReturn("qa");
        when(applicationInfoGateway.findByAppCodeAndDate("APP1", "2024-07-25 10:00:00"))
                .thenReturn(applicationInfo(false));

        VisionProcessResult result = useCase.execute(alert("resolved", "critical"));

        assertEquals("Evento procesado satisfactoriamente", result.message());
        verify(snsGateway).publish(any(AlarmPayload.class));
        verify(notificationGateway, never()).notifyIris(any(AlarmPayload.class));
    }

    @Test
    void doesNotSendFiringAlertOutsideSchedule() {
        when(environment.currentEnvironment()).thenReturn("qa");
        when(applicationInfoGateway.findByAppCodeAndDate("APP1", "2024-07-25 10:00:00"))
                .thenReturn(applicationInfo(false));

        VisionProcessResult result = useCase.execute(alert("firing", "critical"));

        assertEquals("Evento fuera de horario de alertamiento", result.message());
        verify(snsGateway, never()).publish(any(AlarmPayload.class));
        verify(notificationGateway, never()).notifyIris(any(AlarmPayload.class));
    }

    @Test
    void rejectsMissingApplicationCode() {
        VisionAlert invalidAlert = new VisionAlert(
                "firing", "http://vision/alertmanager", "fingerprint", " ",
                "TestAlert", "cluster", "component", "component-id", "pod",
                "critical", "2024-07-25T15:00:00.000Z", null, "description", null
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(invalidAlert));
    }

    @Test
    void rejectsApplicationInfoWithoutNotificationSchedule() {
        when(environment.currentEnvironment()).thenReturn("qa");
        when(applicationInfoGateway.findByAppCodeAndDate("APP1", "2024-07-25 10:00:00"))
                .thenReturn(new ApplicationInfo(Map.of("name", "Example application")));

        assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(alert("firing", "critical"))
        );
    }

    private ApplicationInfo applicationInfo(boolean onSchedule) {
        return new ApplicationInfo(Map.of(
                "name", "Example application",
                "notification", Map.of("onSchedule", onSchedule)
        ));
    }

    private VisionAlert alert(String eventStatus, String severity) {
        return new VisionAlert(
                eventStatus,
                "http://vision/alertmanager",
                "fingerprint",
                "APP1",
                "TestAlert",
                "cluster",
                "component",
                "component-id",
                "Pod",
                severity,
                "2024-07-25T15:00:00.000Z",
                "2024-07-25T15:00:00.000Z",
                "Description",
                "Summary"
        );
    }
}
