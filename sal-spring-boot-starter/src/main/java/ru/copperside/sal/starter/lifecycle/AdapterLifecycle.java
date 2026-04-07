package ru.copperside.sal.starter.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.web.AdapterState;

/**
 * Controls the ordered startup and graceful shutdown of the SAL adapter.
 * <p>
 * C# origin: {@code SalAdapterStartup} — replaces Windows Service lifecycle hooks.
 * <p>
 * Startup sequence:
 * <ol>
 *   <li>RabbitMQ consumers start (managed by Spring AMQP, lower phase)</li>
 *   <li>This lifecycle: mark adapter as started, log ready state</li>
 * </ol>
 * Shutdown sequence:
 * <ol>
 *   <li>Set shutdown flag → OfflineCheckInterceptor rejects new HTTP requests</li>
 *   <li>Spring AMQP drains in-flight messages (configured via {@code spring.rabbitmq.*})</li>
 *   <li>Stop completes</li>
 * </ol>
 */
public class AdapterLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AdapterLifecycle.class);

    /** Phase after Spring AMQP (DEFAULT_PHASE = Integer.MAX_VALUE) but before shutdown drain. */
    private static final int PHASE = Integer.MAX_VALUE - 100;

    private final AdapterState adapterState;
    private final SalProperties properties;

    private volatile boolean running = false;

    public AdapterLifecycle(AdapterState adapterState, SalProperties properties) {
        this.adapterState = adapterState;
        this.properties = properties;
    }

    @Override
    public void start() {
        log.info("SAL adapter starting: name={}", properties.getAdapter().getName());
        running = true;
        // WatchDog will call signalHealthy() once monitors pass, transitioning to online
        log.info("SAL adapter started — waiting for WatchDog monitors to go online");
    }

    @Override
    public void stop() {
        log.info("SAL adapter stopping: initiating graceful shutdown");
        adapterState.setShutDown(true);
        adapterState.setOnline(false);
        running = false;
        log.info("SAL adapter stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
