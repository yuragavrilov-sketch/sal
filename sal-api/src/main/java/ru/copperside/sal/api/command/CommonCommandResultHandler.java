package ru.copperside.sal.api.command;

/**
 * Catch-all result handler — receives results for any command type.
 * <p>
 * C# origin: {@code ICommonCommandResultHandler}
 */
public interface CommonCommandResultHandler {

    boolean completed(CommandResult result, CommandContext context);

    boolean failed(Exception exception, CommandContext context);
}
