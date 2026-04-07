package ru.copperside.sal.starter.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.copperside.sal.api.watchdog.EndPointInfo;
import ru.copperside.sal.starter.SalProperties;

import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe registry of known endpoints.
 * <p>
 * C# origin: {@code endPoints} field + {@code AddEndPoint}/{@code RemoveEndPoint}
 * in {@code WatchDogBaseService}.
 */
public class EndPointRegistry {

    private static final Logger log = LoggerFactory.getLogger(EndPointRegistry.class);

    private final ConcurrentHashMap<String, EndPointInfo> endPoints = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    private final SalProperties properties;

    public EndPointRegistry(SalProperties properties) {
        this.properties = properties;
    }

    public void addOrUpdate(EndPointInfo incoming) {
        endPoints.compute(incoming.getName(), (name, existing) -> {
            if (existing == null) {
                incoming.setId(idSequence.incrementAndGet());
                incoming.setLastPresentCheck(Instant.now());
                incoming.setHostIp(resolveHostIp(incoming.getServerUri()));
                log.info("New endpoint registered: {}", name);
                return incoming;
            }
            // URI changed — reset availability
            if (!existing.getServerUri().equals(incoming.getServerUri())) {
                existing.setServerUri(incoming.getServerUri());
                existing.setPingSucceed(false);
                existing.setLatency(-1);
                existing.setAvailable(false);
                existing.setCheckFailureCount(0);
                existing.setHostIp(resolveHostIp(incoming.getServerUri()));
            }
            if (incoming.getEnvironmentKey() != null &&
                    !incoming.getEnvironmentKey().equals(existing.getEnvironmentKey())) {
                existing.setEnvironmentKey(incoming.getEnvironmentKey());
            }
            existing.setLastPresentCheck(Instant.now());
            return existing;
        });
    }

    public void remove(String name) {
        if (endPoints.remove(name) != null) {
            log.info("Endpoint removed: {}", name);
        }
    }

    public Collection<EndPointInfo> getAll() {
        return endPoints.values();
    }

    /** Update availability based on ping result, SAL version, and online state. */
    public void updateAvailability(EndPointInfo ep) {
        int minVer = properties.getService().getMinEpSalVersion();
        int maxVer = properties.getService().getMaxEpSalVersion();
        boolean nowAvailable = ep.isPingSucceed()
                && ep.isOnline()
                && ep.getSalVersion() > minVer
                && ep.getSalVersion() < maxVer;

        if (nowAvailable && !ep.isAvailable()) {
            log.info("Endpoint {} became available (latency={}ms)", ep.getName(), ep.getLatency());
            ep.setAvailable(true);
            resetUseCounts();
        } else if (!nowAvailable && ep.isAvailable()) {
            log.info("Endpoint {} became unavailable", ep.getName());
            ep.setAvailable(false);
            resetUseCounts();
        }
    }

    private void resetUseCounts() {
        endPoints.values().forEach(e -> e.setUseCount(0));
    }

    private String resolveHostIp(URI uri) {
        if (uri == null) return "unknown";
        String host = uri.getHost();
        if (host.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            return "HOST-" + host.replace(".", "-");
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            return "HOST-" + addr.getHostAddress().replace(".", "-");
        } catch (Exception e) {
            return "HOST-" + host.replace(".", "-");
        }
    }
}
