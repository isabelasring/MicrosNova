package co.com.bancolombia.interpretecloudwatch;

import co.com.bancolombia.model.CloudWatchProcessResult;
import co.com.bancolombia.usecase.ProcessCloudWatchAlertUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
public class CloudWatchController {
    private final ProcessCloudWatchAlertUseCase processCloudWatchAlertUseCase;
    private final ObjectMapper objectMapper;

    public CloudWatchController(
            ProcessCloudWatchAlertUseCase processCloudWatchAlertUseCase,
            ObjectMapper objectMapper
    ) {
        this.processCloudWatchAlertUseCase = processCloudWatchAlertUseCase;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/")
    public CloudWatchProcessResult receiveEvent(@RequestBody Map<String, Object> event) {
        System.out.println("[CloudWatchController] Llega el aviso: " + event);
        CloudWatchProcessResult result = processCloudWatchAlertUseCase.execute(
                CloudWatchMessageReader.read(event.get("Message"), objectMapper)
        );
        System.out.println("[CloudWatchController] Responde: " + result.message());
        return result;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("Test", "Succesful");
    }
}
