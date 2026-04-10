package ru.copperside.sal.starter.rabbitmq;

/**
 * RabbitMQ topology constants matching C# RabbitMQTransport.
 */
public final class SalRabbitConstants {

    private SalRabbitConstants() {}

    /** Command exchange — all commands routed through this exchange (Direct). */
    public static final String COMMAND_EXCHANGE = "CommandExchange";

    /** Command result success exchange (Direct). */
    public static final String COMMAND_COMPLETED_EXCHANGE = "TCB.Infrastructure.Command.CommandCompletedEvent";

    /** Command result failure exchange (Direct). */
    public static final String COMMAND_FAILED_EXCHANGE = "TCB.Infrastructure.Command.CommandFailedEvent";

    /** Dead letter exchange (Fanout). */
    public static final String DEAD_LETTER_EXCHANGE = "dead-letter-exchange";

    /** Dead letter queue. */
    public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";

    /** Queue prefix for command handler queues: "Command_{TypeFullName}". */
    public static final String COMMAND_QUEUE_PREFIX = "Command_";

    /** Queue suffix for command result queues: "{ServiceName}_CommandResult". */
    public static final String COMMAND_RESULT_QUEUE_SUFFIX = "_CommandResult";

    /** Max priority for command queues (x-max-priority). */
    public static final int COMMAND_MAX_PRIORITY = 11;
}
