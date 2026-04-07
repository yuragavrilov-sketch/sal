package ru.copperside.sal.api.command;

import java.util.concurrent.CompletableFuture;

/**
 * Async variant of {@link CommandHandler}.
 * <p>
 * C# origin: {@code ICommandHandlerAsync{TCommand, TCommandResult}}
 *
 * @param <C> command type
 * @param <R> result type
 */
public interface CommandHandlerAsync<C extends Command, R extends CommandResult> {

    CompletableFuture<R> executeAsync(C command);
}
