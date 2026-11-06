package co.com.bancolombia.usecase;

import co.com.bancolombia.model.AlarmPayload;
import co.com.bancolombia.model.VisionAlert;
import co.com.bancolombia.model.VisionProcessResult;
import co.com.bancolombia.model.gateway.VisionEnvironment;
import co.com.bancolombia.model.gateway.VisionSnsGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVisionAlertUseCaseTest {
    private VisionSnsGateway snsGateway;
    private VisionEnvironment environment;
    private ProcessVisionAlertUseCase useCase;

    @BeforeEach
    void setUp() {
        snsGateway = mock(VisionSnsGateway.class);
        environment = mock(VisionEnvironment.class);
        when(environment.currentEnvironment()).thenReturn("dev");
        useCase = new ProcessVisionAlertUseCase(snsGateway, environment);
    }

    @Test
    void publishesEveryAlertToTheIngestionMailbox() {
        VisionProcessResult result = useCase.execute(List.of(
                alert("firing", "critical", "one"),
                alert("resolved", "warning", "two")
        ));

        assertEquals("Evento procesado satisfactoriamente", result.message());
        verify(snsGateway, times(2)).publish(any(AlarmPayload.class));
    }

    @Test
    void rejectsAnEmptyBatch() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(List.of()));
    }

    @Test
    void rejectsMissingApplicationCode() {
        VisionAlert invalidAlert = new VisionAlert(
                "firing", "http://vision/alertmanager", "fingerprint", " ",
                "TestAlert", "cluster", "component", "component-id", "pod",
                "critical", "2024-07-25T15:00:00.000Z", null, "description", null
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(List.of(invalidAlert)));
    }

    private VisionAlert alert(String eventStatus, String severity, String fingerprint) {
        return new VisionAlert(
                eventStatus,
                "http://vision/alertmanager",
                fingerprint,
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
