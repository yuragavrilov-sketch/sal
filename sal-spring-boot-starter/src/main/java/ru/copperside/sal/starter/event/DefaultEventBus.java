package ru.copperside.sal.starter.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;
import ru.copperside.sal.api.event.EventBus;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SessionHolder;
import ru.copperside.sal.starter.session.SessionSerializer;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default {@link EventBus} implementation — publishes events to RabbitMQ.
 * <p>
 * Exchange name = event type full name (C# convention).
 * If event has {@link ServiceMessage}, routingKey = "service".
 * <p>
 * Replaces C# {@code EventProxy.Publish()}.
 */
public class DefaultEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger("EventBus");

    private final RabbitTemplate salRabbitTemplate;
    private final SessionSerializer sessionSerializer;
    private final TypeMappingRegistry typeMappingRegistry;
    private final SalProperties properties;
    private final AtomicLong messageIdCounter = new AtomicLong(0);

    public DefaultEventBus(RabbitTemplate salRabbitTemplate,
                           SessionSerializer sessionSerializer,
                           TypeMappingRegistry typeMappingRegistry,
                           SalProperties properties) {
        this.salRabbitTemplate = salRabbitTemplate;
        this.sessionSerializer = sessionSerializer;
        this.typeMappingRegistry = typeMappingRegistry;
        this.properties = properties;
    }

    @Override
    public void publish(Event... events) {
        for (Event event : events) {
            publish(event);
        }
    }

    @Override
    public void publish(Event event) {
        RecordedMessage rm = buildMessage(event);

        salRabbitTemplate.convertAndSend(
                rm.getExchangeName(),
                rm.getRoutingKey() != null ? rm.getRoutingKey() : "",
                rm);

        log.debug("[EVN -> BUS] {} correlationId={}", rm.getExchangeName(), rm.getCorrelationId());
    }

    @Override
    public void publish(RecordedMessage message) {
        enrichMessage(message);

        salRabbitTemplate.convertAndSend(
                message.getExchangeName(),
                message.getRoutingKey() != null ? message.getRoutingKey() : "",
                message);
    }

    private RecordedMessage buildMessage(Event event) {
        RecordedMessage rm = new RecordedMessage();
        rm.setPayload(event);

        // PayloadType: C# type name if mapped, else Java class name
        String typeName = typeMappingRegistry.resolveCsharpTypeName(event.getClass())
                .orElse(event.getClass().getName());
        rm.setPayloadType(typeName);

        // Exchange = event type full name (C# convention)
        rm.setExchangeName(event.getClass().getName());

        rm.setMessageId(messageIdCounter.incrementAndGet());
        rm.setCorrelationId(UUID.randomUUID().toString());
        rm.setTimeStamp(Instant.now());
        rm.setSourceServiceId(adapterFullName());

        // ServiceMessage → routingKey = "service"
        if (event.getClass().isAnnotationPresent(ServiceMessage.class)) {
            rm.setRoutingKey("service");
        }

        // Session injection
        Map<String, String> additionalData = new HashMap<>();
        Map<String, Object> session = SessionHolder.get();
        if (session != null) {
            try {
                String sessionId = SessionHolder.getSessionId();
                String operationId = SessionHolder.getOperationId();
                if (sessionId != null) additionalData.put("SessionId", sessionId);
                if (operationId != null) additionalData.put("OperationId", operationId);
                additionalData.put("Session", sessionSerializer.serialize(session));
            } catch (IOException e) {
                log.warn("Failed to serialize session for event {}", event.getClass().getSimpleName(), e);
            }
        }
        rm.setAdditionalData(additionalData);

        return rm;
    }

    private void enrichMessage(RecordedMessage rm) {
        if (rm.getMessageId() == 0) rm.setMessageId(messageIdCounter.incrementAndGet());
        if (rm.getCorrelationId() == null) rm.setCorrelationId(UUID.randomUUID().toString());
        if (rm.getTimeStamp() == null) rm.setTimeStamp(Instant.now());
        if (rm.getSourceServiceId() == null) rm.setSourceServiceId(adapterFullName());
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
