package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.copperside.sal.api.command.*;
import ru.copperside.sal.api.exception.ErrorException;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.api.message.MessageDataKeys;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link CommandBus} implementation — facade over CommandPublisher.
 * <p>
 * Replaces C# {@code CommandProxy} public API surface.
 * Manages pending commands for request/reply pattern (ExecuteCommandAsync).
 */
public class DefaultCommandBus implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

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
    public void publishCommand(RecordedMessage recordMessage, String commandName) {
        publisher.publish(recordMessage, commandName);
    }

    @Override
    public void publishCommandResult(CommandResult result, String contextData) {
        // Implemented at the consumer level — result is sent back directly
        throw new UnsupportedOperationException("Use CommandConsumer to send results");
    }

    @Override
    public void publishCommandResult(RecordedMessage result, String contextData) {
        publisher.publishResult(result);
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

    @Override
    @SuppressWarnings("unchecked")
    public <R extends CommandResult> CompletableFuture<ConfirmationResult> confirmatoryCommandAsync(
            HaveResult<R> command, int timeoutSeconds, CommandPriority priority) {

        String commandTypeName = resolveTypeName(command.getClass());
        String correlationId = java.util.UUID.randomUUID().toString();
        Instant expireDate = Instant.now().plusSeconds(timeoutSeconds);

        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingCommands.put(correlationId, new PendingCommand(correlationId, expireDate, future));

        // Publish with confirmation marker
        RecordedMessage rm = new RecordedMessage();
        rm.setCorrelationId(correlationId);
        rm.setPriority((byte) priority.getValue());
        rm.setPayload(command);
        rm.setExpireDate(expireDate);
        rm.getAdditionalData().put(MessageDataKeys.CONFIRMATION, "true");

        publisher.publish(rm, commandTypeName);

        return future.thenApply(result -> (ConfirmationResult) result);
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
