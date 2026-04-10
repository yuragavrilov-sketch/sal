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

    /**
     * Generic request/reply: publish a command identified by its wire type name
     * with an arbitrary payload (POJO, Map, or similar) and await a raw result.
     * <p>
     * Useful for tools that don't have the target command's Java class on the
     * classpath — e.g. a test client poking unknown adapters.
     */
    CompletableFuture<Object> executeCommandAsync(String commandTypeName, Object payload,
                                                   int timeoutSeconds, CommandPriority priority);
}
