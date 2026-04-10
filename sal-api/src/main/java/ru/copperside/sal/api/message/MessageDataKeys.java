package ru.copperside.sal.api.message;

/**
 * Constants for {@link RecordedMessage#getAdditionalData()} keys.
 * <p>
 * Wire-critical: names must match C# {@code AdditionalData} dictionary keys exactly.
 */
public final class MessageDataKeys {

    private MessageDataKeys() {}

    public static final String IS_COMMAND = "IsCommand";
    public static final String NO_CREATE_QUEUE = "NoCreateQueue";
    public static final String SESSION = "Session";
    public static final String SESSION_ID = "SessionId";
    public static final String OPERATION_ID = "OperationId";
    public static final String SOURCE_SERVICE_ID = "SourceServiceId";

    /** C# typo preserved for wire compatibility. */
    public static final String CONFIRMATION = "Confirmtation";

    /** Routing key for service-level events. */
    public static final String SERVICE_ROUTING_KEY = "service";
}
