package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.copperside.sal.api.command.CommandPriority;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SessionHolder;
import ru.copperside.sal.starter.rabbitmq.SalRabbitConstants;
import ru.copperside.sal.starter.session.SessionSerializer;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Constructs and publishes command RecordedMessages via RabbitMQ.
 * <p>
 * Replaces C# {@code CommandProxy.PublishCommand()} methods.
 * No retry buffer (ADR-003) — relies on RabbitTemplate RetryTemplate.
 */
public class CommandPublisher {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

    private final RabbitTemplate salRabbitTemplate;
    private final SessionSerializer sessionSerializer;
    private final SalProperties properties;
    private final AtomicLong messageIdCounter = new AtomicLong(0);

    public CommandPublisher(RabbitTemplate salRabbitTemplate,
                            SessionSerializer sessionSerializer,
                            SalProperties properties) {
        this.salRabbitTemplate = salRabbitTemplate;
        this.sessionSerializer = sessionSerializer;
        this.properties = properties;
    }

    /**
     * Build and publish a command message.
     *
     * @return correlationId
     */
    public String publish(Object command, String commandTypeName,
                          String correlationId, CommandPriority priority,
                          Instant expireDate) {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        RecordedMessage rm = buildMessage(command, commandTypeName, correlationId, priority, expireDate);

        salRabbitTemplate.convertAndSend(
                SalRabbitConstants.COMMAND_EXCHANGE,
                commandTypeName,
                rm);

        log.debug("[SRC -> BUS] Command {} correlationId={}", commandTypeName, correlationId);
        return correlationId;
    }

    /**
     * Publish a pre-built RecordedMessage as a command.
     */
    public void publish(RecordedMessage rm, String commandName) {
        enrichMessage(rm);
        rm.getAdditionalData().put("IsCommand", "");
        rm.setExchangeName(SalRabbitConstants.COMMAND_EXCHANGE);
        rm.setRoutingKey(commandName);
        if (rm.getPayloadType() == null) rm.setPayloadType(commandName);

        salRabbitTemplate.convertAndSend(
                SalRabbitConstants.COMMAND_EXCHANGE,
                commandName,
                rm);
    }

    /**
     * Publish a command result back to the sender adapter.
     */
    public void publishResult(RecordedMessage rm) {
        salRabbitTemplate.convertAndSend(
                rm.getExchangeName(),
                rm.getRoutingKey(),
                rm);
    }

    private RecordedMessage buildMessage(Object command, String commandTypeName,
                                         String correlationId, CommandPriority priority,
                                         Instant expireDate) {
        RecordedMessage rm = new RecordedMessage();
        rm.setCorrelationId(correlationId);
        rm.setPriority((byte) priority.getValue());
        rm.setPayload(command);
        rm.setPayloadType(commandTypeName);
        rm.setRoutingKey(commandTypeName);
        rm.setExchangeName(SalRabbitConstants.COMMAND_EXCHANGE);
        rm.setTimeStamp(Instant.now());
        rm.setSourceServiceId(adapterFullName());
        rm.setMessageId(messageIdCounter.incrementAndGet());
        rm.setExpireDate(expireDate);

        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("IsCommand", "");

        // Session compression into AdditionalData
        Map<String, Object> session = SessionHolder.get();
        if (session != null) {
            try {
                additionalData.put("Session", sessionSerializer.serialize(session));
            } catch (IOException e) {
                log.warn("Failed to serialize session for command {}", commandTypeName, e);
            }
        }

        rm.setAdditionalData(additionalData);
        return rm;
    }

    private void enrichMessage(RecordedMessage rm) {
        if (rm.getTimeStamp() == null) rm.setTimeStamp(Instant.now());
        if (rm.getSourceServiceId() == null) rm.setSourceServiceId(adapterFullName());
        if (rm.getMessageId() == 0) rm.setMessageId(messageIdCounter.incrementAndGet());
        if (rm.getAdditionalData() == null) rm.setAdditionalData(new HashMap<>());

        Map<String, Object> session = SessionHolder.get();
        if (session != null && !rm.getAdditionalData().containsKey("Session")) {
            try {
                rm.getAdditionalData().put("Session", sessionSerializer.serialize(session));
            } catch (IOException e) {
                log.warn("Failed to serialize session", e);
            }
        }
    }

    private String adapterFullName() {
        return properties.getAdapter().getType() + "." + properties.getAdapter().getName();
    }
}
