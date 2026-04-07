package ru.copperside.sal.api.exception;

/**
 * Source type for exception context.
 * <p>
 * C# origin: {@code TCB.SAL.Client.Exceptions.ExceptionSourceType}
 */
public final class ExceptionSourceType {

    private ExceptionSourceType() {}

    public static final String UNKNOWN = "Unknown";
    public static final String SAL = "Sal";
    public static final String ADAPTER = "Adapter";
    public static final String ACTION_PROVIDER = "ActionProvider";
    public static final String EVENT_HANDLER = "EventHandler";
    public static final String COMMAND_HANDLER = "CommandHandler";
    public static final String COMMAND_RESULT_HANDLER = "CommandResultHandler";
}
