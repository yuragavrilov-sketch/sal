package ru.copperside.sal.starter.context;

import ru.copperside.sal.api.command.CommandContext;

/**
 * Thread-local holder for the current command context (ADR-010).
 * <p>
 * Replaces C# {@code CurrentCommand} + {@code CallContext.LogicalSetData("CommandContext")}.
 */
public final class CommandContextHolder {

    private static final ThreadLocal<CommandContext> CURRENT = new ThreadLocal<>();

    private CommandContextHolder() {}

    public static CommandContext get() {
        return CURRENT.get();
    }

    public static void set(CommandContext context) {
        CURRENT.set(context);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
