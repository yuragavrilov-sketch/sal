package ru.copperside.sal.api.command;

/**
 * Command handler that supports two-phase confirmation flow.
 * <p>
 * C# origin: {@code IСonfirmatoryCommandHandler{TCommand, TCommandResult}}
 * (note: original C# had Cyrillic "С" in the name)
 *
 * @param <C> command type
 * @param <R> result type
 */
public interface ConfirmatoryCommandHandler<C extends Command, R extends CommandResult>
        extends CommandHandler<C, R> {

    ConfirmationResult confirmatoryExecute(C command);
}
