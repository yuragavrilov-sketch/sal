package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.api.exception.SalException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Periodically checks for expired pending commands and fails them with CommandExecutionTimeout.
 * <p>
 * Replaces C# {@code CommandProxy.CommandExecutionTimeoutWatcher()}.
 */
public class CommandTimeoutWatcher {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

    private final ConcurrentMap<String, PendingCommand> pendingCommands;

    public CommandTimeoutWatcher(ConcurrentMap<String, PendingCommand> pendingCommands) {
        this.pendingCommands = pendingCommands;
    }

    @Scheduled(fixedDelay = 1000)
    public void checkTimeouts() {
        if (pendingCommands.isEmpty()) return;

        Instant now = Instant.now();
        for (Map.Entry<String, PendingCommand> entry : pendingCommands.entrySet()) {
            PendingCommand pending = entry.getValue();
            if (pending.expireDate() != null && pending.expireDate().isBefore(now)) {
                if (pendingCommands.remove(entry.getKey()) != null) {
                    var ex = SalException.error(SalErrorCodes.COMMAND_EXECUTION_TIMEOUT, "Command execution timeout");
                    pending.future().completeExceptionally(ex);
                    log.warn("Command timeout: correlationId={}", entry.getKey());
                }
            }
        }
    }

    /**
     * Fail all pending commands (called on shutdown).
     */
    public void abortAll() {
        for (Map.Entry<String, PendingCommand> entry : pendingCommands.entrySet()) {
            if (pendingCommands.remove(entry.getKey()) != null) {
                var ex = SalException.error(SalErrorCodes.COMMAND_ABORTED, "Command aborted");
                entry.getValue().future().completeExceptionally(ex);
            }
        }
    }
}
