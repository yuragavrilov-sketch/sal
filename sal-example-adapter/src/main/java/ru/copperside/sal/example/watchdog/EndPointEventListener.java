package ru.copperside.sal.example.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.copperside.sal.api.event.endpoint.EndPointPresentEvent;
import ru.copperside.sal.api.watchdog.EndPointInfo;
import ru.copperside.sal.starter.watchdog.EndPointRegistry;

import java.net.URI;

/**
 * Registers/updates endpoints from incoming SAL bus events.
 * <p>
 * C# origin: {@code WatchDogAdapterService.Handle(EndPointUpEvent)} —
 * endpoint discovery via event bus.
 */
@Component
public class EndPointEventListener {

    private static final Logger log = LoggerFactory.getLogger(EndPointEventListener.class);

    private final EndPointRegistry endPointRegistry;

    public EndPointEventListener(EndPointRegistry endPointRegistry) {
        this.endPointRegistry = endPointRegistry;
    }

    @EventListener
    public void onEndPointPresent(EndPointPresentEvent event) {
        if (event.getEndPointName() == null || event.getEndPointUri() == null) return;
        EndPointInfo ep = new EndPointInfo();
        ep.setName(event.getEndPointName());
        try {
            ep.setServerUri(new URI(event.getEndPointUri()));
        } catch (Exception e) {
            log.warn("Invalid endpoint URI from {}: {}", event.getEndPointName(), event.getEndPointUri());
            return;
        }
        ep.setEnvironmentKey(event.getEnvironmentKey());
        ep.setSalVersion(event.getSalVersion());
        endPointRegistry.addOrUpdate(ep);
    }
}
