package co.com.bancolombia.interpretevision;

import co.com.bancolombia.model.VisionProcessResult;
import co.com.bancolombia.usecase.ProcessVisionAlertUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class VisionController {
    private final ProcessVisionAlertUseCase processVisionAlertUseCase;

    public VisionController(ProcessVisionAlertUseCase processVisionAlertUseCase) {
        this.processVisionAlertUseCase = processVisionAlertUseCase;
    }

    @PostMapping("/")
    public VisionProcessResult receiveEvent(@Valid @RequestBody VisionWebhookRequest request) {
        return processVisionAlertUseCase.execute(request.toVisionAlert());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("Test", "Succesful");
    }
}
