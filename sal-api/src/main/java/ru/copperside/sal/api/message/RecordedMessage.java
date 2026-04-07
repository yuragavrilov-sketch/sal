package ru.copperside.sal.api.message;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Wire-format wrapper for all RabbitMQ messages (commands, events, results).
 * <p>
 * Wire-critical: PascalCase JSON via wireObjectMapper (ADR-002).
 * <p>
 * C# origin: {@code TCB.Infrastructure.Message.RecordedMessage}
 */
public class RecordedMessage {

    private Object payload;
    private String payloadType;
    private String exchangeName;
    private String routingKey;
    private long messageId;
    private String correlationId;
    private String sourceServiceId;
    private byte priority;
    private Instant timeStamp;
    private Instant expireDate;
    private Map<String, String> additionalData = new HashMap<>();

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSourceServiceId() { return sourceServiceId; }
    public void setSourceServiceId(String sourceServiceId) { this.sourceServiceId = sourceServiceId; }

    public byte getPriority() { return priority; }
    public void setPriority(byte priority) { this.priority = priority; }

    public Instant getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Instant timeStamp) { this.timeStamp = timeStamp; }

    public Instant getExpireDate() { return expireDate; }
    public void setExpireDate(Instant expireDate) { this.expireDate = expireDate; }

    public Map<String, String> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, String> additionalData) { this.additionalData = additionalData; }
}
