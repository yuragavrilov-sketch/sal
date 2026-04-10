package ru.copperside.sal.example.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.api.command.CommandPriority;

/**
 * Demonstrates end-to-end command execution via RabbitMQ at startup.
 * <p>
 * Publishes 3 {@link EchoCommand} instances, waits for their results, and logs
 * the round-trip. The adapter keeps running after the demo so you can observe
 * the command listener loop — stop with Ctrl+C.
 * <p>
 * Runs on a background thread with a short startup delay to ensure
 * {@code CommandListenerRegistrar} has finished declaring queues and starting
 * listener containers (both run on {@link ApplicationReadyEvent} — event order
 * is non-deterministic, so we decouple with a thread).
 */
@Component
@ConditionalOnProperty(name = "sal.example.echo-runner.enabled", havingValue = "true", matchIfMissing = true)
public class EchoRunner {

    private static final Logger log = LoggerFactory.getLogger(EchoRunner.class);
    private static final long STARTUP_DELAY_MS = 2000;

    private final CommandBus commandBus;

    public EchoRunner(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread demo = new Thread(this::runDemo, "echo-demo");
        demo.setDaemon(true);
        demo.start();
    }

    private void runDemo() {
        try {
            Thread.sleep(STARTUP_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        log.info("=== Example adapter CLI demo ===");

        for (int i = 1; i <= 3; i++) {
            EchoCommand command = new EchoCommand();
            command.setPayload("hello #" + i);

            try {
                EchoResult result = commandBus
                        .<EchoResult>executeCommandAsync(command, 10, CommandPriority.Normal)
                        .get();
                log.info("Echo round-trip #{}: '{}' → '{}'", i, command.getPayload(), result.getEcho());
            } catch (Exception e) {
                log.error("Echo round-trip #{} failed", i, e);
            }
        }

        log.info("=== Demo complete. Adapter remains running; Ctrl+C to exit. ===");
    }
}
