package ru.copperside.sal.api.command;

/**
 * Command that declares its result type.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.IHaveResult{TCommandResult}}
 *
 * @param <R> result type
 */
public interface HaveResult<R extends CommandResult> extends Command {
}
