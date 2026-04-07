package ru.copperside.sal.api.command;

/**
 * Handles command results (success or failure) on the sender side.
 * <p>
 * C# origin: {@code ICommandResultHandler{TCommand, TResult}}
 *
 * @param <C> command type
 * @param <R> result type
 */
public interface CommandResultHandler<C extends HaveResult<R>, R extends CommandResult> {

    boolean completed(R result, CommandContext context);

    boolean failed(Exception exception, CommandContext context);
}
