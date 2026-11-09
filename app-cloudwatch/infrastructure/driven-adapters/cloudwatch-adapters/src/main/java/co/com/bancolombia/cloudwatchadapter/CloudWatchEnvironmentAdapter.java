package co.com.bancolombia.cloudwatchadapter;

import co.com.bancolombia.model.gateway.CloudWatchEnvironment;

public class CloudWatchEnvironmentAdapter implements CloudWatchEnvironment {
    private final String environment;

    public CloudWatchEnvironmentAdapter(String environment) {
        this.environment = environment;
    }

    @Override
    public String currentEnvironment() {
        return environment;
    }
}
