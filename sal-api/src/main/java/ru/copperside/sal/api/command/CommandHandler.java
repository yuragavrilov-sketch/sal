package ru.copperside.sal.api.command;

/**
 * Handles a command and returns a result.
 * <p>
 * C# origin: {@code ICommandHandler{TCommand, TCommandResult}} +
 *            {@code ICommandHandlerAsync{TCommand, TCommandResult}}
 * <p>
 * Java unifies sync/async — always returns CompletableFuture in the bus layer,
 * but handler implementations may be synchronous.
 *
 * @param <C> command type
 * @param <R> result type
 */
public interface CommandHandler<C extends Command, R extends CommandResult> {

    R execute(C command);
}
