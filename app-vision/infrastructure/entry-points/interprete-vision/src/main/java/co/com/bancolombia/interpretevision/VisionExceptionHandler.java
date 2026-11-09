package co.com.bancolombia.interpretevision;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestControllerAdvice
public class VisionExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEvent(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("detail", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleDependencyFailure(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("detail", exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleHttpClientFailure(RestClientException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("detail", "Falló la comunicación con un servicio externo"));
    }
}
