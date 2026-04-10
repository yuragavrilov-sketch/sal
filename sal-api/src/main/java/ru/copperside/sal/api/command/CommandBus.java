package ru.copperside.sal.api.command;

import java.util.concurrent.CompletableFuture;

/**
 * Command Bus — send and execute commands across adapters via RabbitMQ.
 */
public interface CommandBus {

    /** Fire-and-forget: publish command, don't wait for result. */
    String publishCommand(Command command, String correlationId, CommandPriority priority);

    default String publishCommand(Command command) {
        return publishCommand(command, "", CommandPriority.Normal);
    }

    /** Request/reply: send command and wait for result with timeout. */
    <R extends CommandResult> CompletableFuture<R> executeCommandAsync(
            HaveResult<R> command, int timeoutSeconds, CommandPriority priority);

    default <R extends CommandResult> CompletableFuture<R> executeCommandAsync(HaveResult<R> command) {
        return executeCommandAsync(command, 120, CommandPriority.Normal);
    }
}
