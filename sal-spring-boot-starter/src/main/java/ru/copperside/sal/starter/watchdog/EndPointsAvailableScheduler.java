package ru.copperside.sal.starter.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.api.watchdog.EndPointInfo;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.client.SalRestClient;

import java.util.Collection;

/**
 * Pings all registered endpoints on a schedule and updates their availability.
 * <p>
 * C# origin: {@code EndPointsAvailableMonitor.Alive()} — replaces Timer-based scheduling
 * with Spring {@code @Scheduled}.
 */
public class EndPointsAvailableScheduler {

    private static final Logger log = LoggerFactory.getLogger(EndPointsAvailableScheduler.class);

    private final EndPointRegistry registry;
    private final WatchDogService watchDogService;
    private final SalRestClient salRestClient;
    private final SalProperties properties;

    public EndPointsAvailableScheduler(EndPointRegistry registry,
                                       WatchDogService watchDogService,
                                       SalRestClient salRestClient,
                                       SalProperties properties) {
        this.registry = registry;
        this.watchDogService = watchDogService;
        this.salRestClient = salRestClient;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${sal.watchdog.ping-interval-ms:10000}")
    public void checkEndPoints() {
        Collection<EndPointInfo> endPoints = registry.getAll();
        if (endPoints.isEmpty()) {
            return;
        }

        String adapterName = properties.getAdapter().getName();

        endPoints.parallelStream().forEach(ep -> pingEndPoint(ep, adapterName));

        boolean anyAvailable = endPoints.stream().anyMatch(EndPointInfo::isAvailable);
        if (anyAvailable) {
            watchDogService.signalHealthy();
        } else {
            watchDogService.signalUnhealthy("No available endpoints");
        }
    }

    private void pingEndPoint(EndPointInfo ep, String adapterName) {
        long startMs = System.currentTimeMillis();
        try {
            PingResponse response = salRestClient.ping(ep.getServerUri(), ep.getEnvironmentKey(), adapterName);
            long elapsed = System.currentTimeMillis() - startMs;

            if (ep.getName().equals(response.getRecipientServiceName())) {
                ep.setLatency(elapsed);
                ep.setPingSucceed(true);
                ep.setOnline(response.isOnline());
                ep.setSalVersion(response.getSalVersion() != null ? response.getSalVersion() : 0);
                ep.setCheckFailureCount(0);
            } else {
                log.warn("Endpoint name mismatch: expected={} got={}", ep.getName(), response.getRecipientServiceName());
                ep.setPingSucceed(false);
                ep.setLatency(-1);
                ep.setCheckFailureCount(ep.getCheckFailureCount() + 1);
            }
        } catch (Exception e) {
            log.warn("Ping failed for endpoint {}: {}", ep.getName(), e.getMessage());
            ep.setPingSucceed(false);
            ep.setLatency(-1);
            ep.setCheckFailureCount(ep.getCheckFailureCount() + 1);
        }
        registry.updateAvailability(ep);
    }
}
