package ru.copperside.sal.api.command;

import java.util.concurrent.CompletableFuture;

/**
 * Async variant of {@link ConfirmatoryCommandHandler}.
 * <p>
 * C# origin: {@code IСonfirmatoryCommandHandlerAsync{TCommand, TCommandResult}}
 *
 * @param <C> command type
 * @param <R> result type
 */
public interface ConfirmatoryCommandHandlerAsync<C extends Command, R extends CommandResult>
        extends CommandHandlerAsync<C, R> {

    CompletableFuture<ConfirmationResult> confirmatoryExecuteAsync(C command);
}
