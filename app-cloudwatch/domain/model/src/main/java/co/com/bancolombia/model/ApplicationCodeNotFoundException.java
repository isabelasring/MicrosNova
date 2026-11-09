package co.com.bancolombia.model;

public class ApplicationCodeNotFoundException extends RuntimeException {
    public ApplicationCodeNotFoundException(String message) {
        super(message);
    }
}
