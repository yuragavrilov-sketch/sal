package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.copperside.sal.api.command.CommandPriority;
import ru.copperside.sal.api.message.MessageDataKeys;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SalContext;
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
    private final String adapterFullName;
    private final AtomicLong messageIdCounter = new AtomicLong(0);

    public CommandPublisher(RabbitTemplate salRabbitTemplate,
                            SessionSerializer sessionSerializer,
                            SalProperties properties) {
        this.salRabbitTemplate = salRabbitTemplate;
        this.sessionSerializer = sessionSerializer;
        this.adapterFullName = properties.getAdapter().getType() + "." + properties.getAdapter().getName();
    }

    /**
     * Build and publish a command message.
     * <p>
     * {@code commandTypeName} may be either the bare C# FQN (used as the
     * routing key) or the assembly-qualified form {@code "<FQN>, <Assembly>"}.
     * In the latter case the FQN part is used as the routing key and the full
     * string is preserved in {@code content_type} — that is what SAL C#
     * consumers expect on the wire.
     *
     * @return correlationId
     */
    public String publish(Object command, String commandTypeName,
                          String correlationId, CommandPriority priority,
                          Instant expireDate) {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String routingKey = stripAssemblySuffix(commandTypeName);

        RecordedMessage rm = buildMessage(command, commandTypeName, routingKey,
                correlationId, priority, expireDate);

        salRabbitTemplate.convertAndSend(
                SalRabbitConstants.COMMAND_EXCHANGE,
                routingKey,
                rm);

        log.debug("[SRC -> BUS] Command {} correlationId={}", commandTypeName, correlationId);
        return correlationId;
    }

    private static String stripAssemblySuffix(String typeName) {
        if (typeName == null) return null;
        int comma = typeName.indexOf(',');
        return comma < 0 ? typeName : typeName.substring(0, comma).trim();
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

    private RecordedMessage buildMessage(Object command, String commandTypeName, String routingKey,
                                         String correlationId, CommandPriority priority,
                                         Instant expireDate) {
        RecordedMessage rm = new RecordedMessage();
        rm.setCorrelationId(correlationId);
        rm.setPriority((byte) priority.getValue());
        rm.setPayload(command);
        rm.setPayloadType(commandTypeName); // full "<FQN>, <Assembly>" form if provided
        rm.setRoutingKey(routingKey);
        rm.setExchangeName(SalRabbitConstants.COMMAND_EXCHANGE);
        rm.setTimeStamp(Instant.now());
        rm.setSourceServiceId(adapterFullName);
        rm.setMessageId(messageIdCounter.incrementAndGet());
        rm.setExpireDate(expireDate);

        Map<String, String> additionalData = new HashMap<>();
        additionalData.put(MessageDataKeys.IS_COMMAND, "");
        additionalData.put(MessageDataKeys.NO_CREATE_QUEUE, "");

        // C# SAL framework's SessionHelper.GetSID() dereferences Session unconditionally
        // while building error DTOs — if Session is missing or wrong-shaped, an NPE
        // inside the error handler swallows the real exception AND prevents
        // FailedResult from being published. Always send a synthetic session in the
        // exact shape C# Newtonsoft.Json expects:
        //   {"__type":"DictionaryData","sessionid":"...","operationid":N,"authid":N}
        // __type is the polymorphic deserialization hint, keys are lowercase.
        Map<String, Object> session = SalContext.session();
        if (session == null || session.isEmpty()) {
            session = new java.util.LinkedHashMap<>();
            session.put("__type", "DictionaryData");
            session.put("sessionid", adapterFullName);
            session.put("operationid", 0);
            session.put("authid", 0);
        }
        try {
            additionalData.put(MessageDataKeys.SESSION, sessionSerializer.serialize(session));
        } catch (IOException e) {
            log.warn("Failed to serialize session for command {}", commandTypeName, e);
        }

        rm.setAdditionalData(additionalData);
        return rm;
    }

    private void enrichMessage(RecordedMessage rm) {
        if (rm.getTimeStamp() == null) rm.setTimeStamp(Instant.now());
        if (rm.getSourceServiceId() == null) rm.setSourceServiceId(adapterFullName);
        if (rm.getMessageId() == 0) rm.setMessageId(messageIdCounter.incrementAndGet());
        if (rm.getAdditionalData() == null) rm.setAdditionalData(new HashMap<>());

        Map<String, Object> session = SalContext.session();
        if (session != null && !rm.getAdditionalData().containsKey(MessageDataKeys.SESSION)) {
            try {
                rm.getAdditionalData().put(MessageDataKeys.SESSION, sessionSerializer.serialize(session));
            } catch (IOException e) {
                log.warn("Failed to serialize session", e);
            }
        }
    }

}
