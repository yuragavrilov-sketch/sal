package ru.copperside.sal.starter.command;

import ru.copperside.sal.api.command.Command;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.api.command.CommandPriority;
import ru.copperside.sal.api.command.CommandResult;
import ru.copperside.sal.api.command.HaveResult;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link CommandBus} implementation — facade over CommandPublisher.
 * Manages pending commands for request/reply pattern (executeCommandAsync).
 */
public class DefaultCommandBus implements CommandBus {

    private final CommandPublisher publisher;
    private final TypeMappingRegistry typeMappingRegistry;
    final ConcurrentMap<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    public DefaultCommandBus(CommandPublisher publisher, TypeMappingRegistry typeMappingRegistry) {
        this.publisher = publisher;
        this.typeMappingRegistry = typeMappingRegistry;
    }

    @Override
    public String publishCommand(Command command, String correlationId, CommandPriority priority) {
        String commandTypeName = resolveTypeName(command.getClass());
        return publisher.publish(command, commandTypeName, correlationId, priority, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends CommandResult> CompletableFuture<R> executeCommandAsync(
            HaveResult<R> command, int timeoutSeconds, CommandPriority priority) {

        String commandTypeName = resolveTypeName(command.getClass());
        String correlationId = java.util.UUID.randomUUID().toString();
        Instant expireDate = Instant.now().plusSeconds(timeoutSeconds);

        // Register pending command
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingCommands.put(correlationId, new PendingCommand(correlationId, expireDate, future));

        // Publish
        try {
            publisher.publish(command, commandTypeName, correlationId, priority, expireDate);
        } catch (Exception e) {
            pendingCommands.remove(correlationId);
            future.completeExceptionally(e);
        }

        return future.thenApply(result -> (R) result);
    }

    /**
     * Called by CommandResultConsumer when a result arrives for a pending command.
     *
     * @return true if the result was consumed (correlationId found)
     */
    public boolean completePendingCommand(String correlationId, Object result) {
        PendingCommand pending = pendingCommands.remove(correlationId);
        if (pending != null) {
            pending.future().complete(result);
            return true;
        }
        return false;
    }

    /**
     * Called by CommandResultConsumer when a command fails.
     */
    public boolean failPendingCommand(String correlationId, Exception exception) {
        PendingCommand pending = pendingCommands.remove(correlationId);
        if (pending != null) {
            pending.future().completeExceptionally(exception);
            return true;
        }
        return false;
    }

    private String resolveTypeName(Class<?> commandClass) {
        return typeMappingRegistry.resolveCsharpTypeName(commandClass)
                .orElse(commandClass.getName());
    }
}
