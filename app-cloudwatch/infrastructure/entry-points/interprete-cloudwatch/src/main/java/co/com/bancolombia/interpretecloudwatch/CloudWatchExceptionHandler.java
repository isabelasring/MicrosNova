package co.com.bancolombia.interpretecloudwatch;

import co.com.bancolombia.model.AlarmDataException;
import co.com.bancolombia.model.ApplicationCodeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestControllerAdvice
public class CloudWatchExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEvent(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("detail", exception.getMessage()));
    }

    @ExceptionHandler(ApplicationCodeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMissingApplicationCode(ApplicationCodeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", exception.getMessage()));
    }

    @ExceptionHandler(AlarmDataException.class)
    public ResponseEntity<Map<String, String>> handleIncompleteAlarm(AlarmDataException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("detail", exception.getMessage()));
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
