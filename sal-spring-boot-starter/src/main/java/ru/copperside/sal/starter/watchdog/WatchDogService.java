package ru.copperside.sal.starter.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import ru.copperside.sal.api.event.adapter.AdapterOfflineEvent;
import ru.copperside.sal.api.event.adapter.AdapterOnlineEvent;
import ru.copperside.sal.api.watchdog.EndPointInfo;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.web.AdapterState;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages adapter online/offline transitions based on WatchDog monitor results.
 * <p>
 * C# origin: {@code WatchDogBaseService.OnOnline()} / {@code OnOffline()} logic.
 * <p>
 * In Java, instead of a custom timer loop, each monitor runs via Spring {@code @Scheduled}.
 * This service receives status updates from monitors and decides the adapter state.
 */
public class WatchDogService {

    private static final Logger log = LoggerFactory.getLogger(WatchDogService.class);

    private final AdapterState adapterState;
    private final EndPointRegistry endPointRegistry;
    private final SalProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    /** Manual online switch (managed by SwitchController). True = online allowed. */
    private final AtomicBoolean manualOnlineSwitch = new AtomicBoolean(true);

    public WatchDogService(AdapterState adapterState,
                           EndPointRegistry endPointRegistry,
                           SalProperties properties,
                           ApplicationEventPublisher eventPublisher) {
        this.adapterState = adapterState;
        this.endPointRegistry = endPointRegistry;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    /** Called by monitors when all checks pass. */
    public synchronized void signalHealthy() {
        if (!manualOnlineSwitch.get()) return;
        if (!adapterState.isOnline()) {
            adapterState.setOnline(true);
            log.info("Adapter went ONLINE");
            publishOnlineEvent();
        }
    }

    /** Called by monitors when a check fails. */
    public synchronized void signalUnhealthy(String reason) {
        if (adapterState.isOnline()) {
            adapterState.setOnline(false);
            log.warn("Adapter went OFFLINE: {}", reason);
            publishOfflineEvent();
        }
    }

    /** Toggle online/offline manually (e.g., from SwitchController). */
    public synchronized boolean toggleOnline() {
        boolean previous = manualOnlineSwitch.get();
        boolean newValue = !previous;
        manualOnlineSwitch.set(newValue);
        log.info("Manual online switch: {}", newValue);
        if (!newValue) {
            signalUnhealthy("manual switch");
        }
        return newValue;
    }

    public boolean isManualOnline() {
        return manualOnlineSwitch.get();
    }

    public Collection<EndPointInfo> getEndPoints() {
        return endPointRegistry.getAll();
    }

    private void publishOnlineEvent() {
        AdapterOnlineEvent event = new AdapterOnlineEvent();
        event.setAdapterName(properties.getAdapter().getName());
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish AdapterOnlineEvent: {}", e.getMessage());
        }
    }

    private void publishOfflineEvent() {
        AdapterOfflineEvent event = new AdapterOfflineEvent();
        event.setAdapterName(properties.getAdapter().getName());
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish AdapterOfflineEvent: {}", e.getMessage());
        }
    }
}
