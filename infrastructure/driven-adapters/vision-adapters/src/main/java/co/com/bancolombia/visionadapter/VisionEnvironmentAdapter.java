package co.com.bancolombia.visionadapter;

import co.com.bancolombia.model.gateway.VisionEnvironment;

public class VisionEnvironmentAdapter implements VisionEnvironment {
    private final String environment;

    public VisionEnvironmentAdapter(String environment) {
        this.environment = environment;
    }

    @Override
    public String currentEnvironment() {
        return environment;
    }
}
