package ru.copperside.sal.api.message;

import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message that was rejected during processing.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Message.MessageRejected}
 */
public class MessageRejected {

    private String payloadType;
    private byte[] payload;
    private String exchangeName;
    private String routingKey;
    private String sourceQueue;
    private String correlationId;
    private String messageId;
    private byte priority;
    private Instant timestamp;
    private Map<String, String> headerData = new HashMap<>();
    private InfrastructureExceptionDTO exceptionData;

    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }

    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getSourceQueue() { return sourceQueue; }
    public void setSourceQueue(String sourceQueue) { this.sourceQueue = sourceQueue; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public byte getPriority() { return priority; }
    public void setPriority(byte priority) { this.priority = priority; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getHeaderData() { return headerData; }
    public void setHeaderData(Map<String, String> headerData) { this.headerData = headerData; }

    public InfrastructureExceptionDTO getExceptionData() { return exceptionData; }
    public void setExceptionData(InfrastructureExceptionDTO exceptionData) { this.exceptionData = exceptionData; }
}
