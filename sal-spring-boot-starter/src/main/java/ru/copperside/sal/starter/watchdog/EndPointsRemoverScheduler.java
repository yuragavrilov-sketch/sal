package ru.copperside.sal.starter.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import ru.copperside.sal.api.watchdog.EndPointInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Removes endpoints that have not been seen for the configured inactivity period.
 * <p>
 * C# origin: {@code EndPointsRemoverMonitor} — 30-minute inactivity threshold.
 */
public class EndPointsRemoverScheduler {

    private static final Logger log = LoggerFactory.getLogger(EndPointsRemoverScheduler.class);
    private static final Duration INACTIVITY_THRESHOLD = Duration.ofMinutes(30);

    private final EndPointRegistry registry;

    public EndPointsRemoverScheduler(EndPointRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${sal.watchdog.remover-interval-ms:60000}")
    public void removeStaleEndPoints() {
        Instant cutoff = Instant.now().minus(INACTIVITY_THRESHOLD);
        List<String> stale = registry.getAll().stream()
                .filter(ep -> ep.getLastPresentCheck() != null && ep.getLastPresentCheck().isBefore(cutoff))
                .map(EndPointInfo::getName)
                .toList();
        stale.forEach(name -> {
            log.info("Removing stale endpoint: {}", name);
            registry.remove(name);
        });
    }
}
