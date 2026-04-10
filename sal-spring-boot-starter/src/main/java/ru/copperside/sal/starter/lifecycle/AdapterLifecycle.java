package ru.copperside.sal.starter.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.command.CommandTimeoutWatcher;

public class AdapterLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AdapterLifecycle.class);
    private static final int PHASE = Integer.MAX_VALUE - 100;

    private final String adapterName;
    private final CommandTimeoutWatcher timeoutWatcher;
    private volatile boolean running = false;

    public AdapterLifecycle(SalProperties properties, CommandTimeoutWatcher timeoutWatcher) {
        this.adapterName = properties.getAdapter().getName();
        this.timeoutWatcher = timeoutWatcher;
    }

    @Override
    public void start() {
        running = true;
        log.info("SAL adapter started: name={}", adapterName);
    }

    @Override
    public void stop() {
        running = false;
        timeoutWatcher.abortAll();
        log.info("SAL adapter stopped: name={}", adapterName);
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public int getPhase() { return PHASE; }

    @Override
    public boolean isAutoStartup() { return true; }
}
