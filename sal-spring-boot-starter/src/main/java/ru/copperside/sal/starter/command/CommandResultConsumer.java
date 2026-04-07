package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import ru.copperside.sal.api.command.FailedResult;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.rabbitmq.SalMessageConverter;

/**
 * Receives command results from the {@code {adapterFullName}_CommandResult} queue.
 * <p>
 * Completes or fails pending {@link java.util.concurrent.CompletableFuture} instances
 * registered in {@link DefaultCommandBus#pendingCommands}.
 * <p>
 * Replaces C# {@code CommandResultProcessor}.
 */
public class CommandResultConsumer implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

    private final SalMessageConverter messageConverter;
    private final DefaultCommandBus commandBus;

    public CommandResultConsumer(SalMessageConverter messageConverter,
                                 DefaultCommandBus commandBus) {
        this.messageConverter = messageConverter;
        this.commandBus = commandBus;
    }

    @Override
    public void onMessage(Message message) {
        RecordedMessage rm = (RecordedMessage) messageConverter.fromMessage(message);
        String correlationId = rm.getCorrelationId();

        if (correlationId == null) {
            log.warn("[BUS] Command result received without correlationId, ignoring");
            return;
        }

        Object payload = rm.getPayload();
        if (payload instanceof FailedResult failedResult) {
            String error = failedResult.getExeption(); // C# typo preserved
            boolean consumed = commandBus.failPendingCommand(correlationId,
                    new RuntimeException("Command failed: " + error));
            if (!consumed) {
                log.warn("[BUS] Failed result for unknown correlationId={}", correlationId);
            } else {
                log.debug("[BUS] Failed result delivered correlationId={}", correlationId);
            }
        } else {
            boolean consumed = commandBus.completePendingCommand(correlationId, payload);
            if (!consumed) {
                log.debug("[BUS] Result for unknown/expired correlationId={}", correlationId);
            } else {
                log.debug("[BUS] Result delivered correlationId={}", correlationId);
            }
        }
    }
}
