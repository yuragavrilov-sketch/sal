package ru.copperside.sal.api.command;

import ru.copperside.sal.api.message.RecordedMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Command Bus — send and execute commands across adapters via RabbitMQ.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.ICommandBus}
 */
public interface CommandBus {

    /** Fire-and-forget: publish command, don't wait for result. */
    String publishCommand(Command command, String correlationId, CommandPriority priority);

    default String publishCommand(Command command) {
        return publishCommand(command, "", CommandPriority.Normal);
    }

    /** Publish a pre-built RecordedMessage as a command. */
    void publishCommand(RecordedMessage recordMessage, String commandName);

    /** Publish command result back to the sender. */
    void publishCommandResult(CommandResult result, String contextData);

    /** Publish a pre-built RecordedMessage as a command result. */
    void publishCommandResult(RecordedMessage result, String contextData);

    /** Request/reply: send command and wait for result with timeout. */
    <R extends CommandResult> CompletableFuture<R> executeCommandAsync(
            HaveResult<R> command, int timeoutSeconds, CommandPriority priority);

    default <R extends CommandResult> CompletableFuture<R> executeCommandAsync(HaveResult<R> command) {
        return executeCommandAsync(command, 120, CommandPriority.Normal);
    }

}
