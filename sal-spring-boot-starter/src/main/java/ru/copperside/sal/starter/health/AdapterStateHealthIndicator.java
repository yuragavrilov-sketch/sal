package ru.copperside.sal.starter.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.watchdog.EndPointRegistry;
import ru.copperside.sal.starter.web.AdapterState;

/**
 * Exposes adapter online/offline state and endpoint summary via Spring Boot Actuator health endpoint.
 */
public class AdapterStateHealthIndicator implements HealthIndicator {

    private final AdapterState adapterState;
    private final EndPointRegistry endPointRegistry;
    private final SalProperties properties;

    public AdapterStateHealthIndicator(AdapterState adapterState,
                                       EndPointRegistry endPointRegistry,
                                       SalProperties properties) {
        this.adapterState = adapterState;
        this.endPointRegistry = endPointRegistry;
        this.properties = properties;
    }

    @Override
    public Health health() {
        boolean online = adapterState.isOnline();
        long totalEndPoints = endPointRegistry.getAll().size();
        long availableEndPoints = endPointRegistry.getAll().stream()
                .filter(ep -> ep.isAvailable()).count();

        Health.Builder builder = online ? Health.up() : Health.down();
        builder.withDetail("adapter", properties.getAdapter().getName())
               .withDetail("online", online)
               .withDetail("shutdown", adapterState.isShutDown())
               .withDetail("endpoints.total", totalEndPoints)
               .withDetail("endpoints.available", availableEndPoints);

        return builder.build();
    }
}
