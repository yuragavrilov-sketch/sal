package ru.copperside.sal.api.exception;

/**
 * Error codes used in the SAL exception model.
 * <p>
 * Wire-critical: these string values are parsed by C# clients.
 * <p>
 * C# origin: {@code TCB.SAL.Client.Exceptions.SalErrorCodes}
 */
public final class SalErrorCodes {

    private SalErrorCodes() {}

    public static final String FATAL_EXCEPTION = "FatalException";
    public static final String EXECUTING_METHOD_NOT_FOUND = "ExecutingMethodNotFound";
    public static final String PATH_NOT_FOUND = "PathNotFound";
    public static final String ROUTE_NOT_FOUND = "RouteNotFound";
    public static final String MISMATCH_ENVIRONMENT_KEY = "MismatchEnvironmentKey";
    public static final String NO_AVAILABLE_ENDPOINT = "NoAvailableEndpoint";
    public static final String NO_AVAILABLE_ADAPTER = "NoAvailableAdapter";
    public static final String NO_AVAILABLE_MESSAGE_BUS = "NoAvailableMessageBus";
    public static final String SERVICE_CONNECT_FAILURE = "ServiceConnectFailure";
    public static final String SERVICE_TIMEOUT = "ServiceTimeout";
    public static final String WEB_SERVICE = "WebService";
    public static final String ADAPTER_IS_OFFLINE = "AdapterIsOffline";
    public static final String INVALID_COMMAND_CONTEXT_DATA = "InvalidCommandContextData";
    public static final String UNKNOWN_COMMAND_RESULT_TYPE = "UnknownCommandResultType";
    public static final String COMMAND_NOT_PROCESSING = "CommandNotProcessing";
    public static final String NOT_HANDLED_COMMAND_RESULT = " NotHandledCommandResult"; // leading space preserved from C#
    public static final String NOT_HANDLED_COMMAND = "NotHandledCommand";
    public static final String NOT_HANDLED_EVENT = "NotHandledEvent";
    public static final String CONNECTION_CREATOR_CONFIG = "ConnectionCreatorConfig";
    public static final String SESSION_REQUIRED = "SessionRequired";
    public static final String COMMAND_EXECUTION_TIMEOUT = "CommandExecutionTimeout";
    public static final String COMMAND_ABORTED = "CommandAborted";
}
