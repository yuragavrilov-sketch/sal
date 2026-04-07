package ru.copperside.sal.api.event;

import java.time.Instant;

/**
 * Metadata about the origin of an event.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Event.EventSource}
 */
public class EventSource {

    private String eventSourceServiceId;
    private String eventSessionId;
    private long eventOperationId;
    private long eventMessageId;
    private String eventCorrelationId;
    private Instant eventTimeStamp;

    public String getEventSourceServiceId() { return eventSourceServiceId; }
    public void setEventSourceServiceId(String eventSourceServiceId) { this.eventSourceServiceId = eventSourceServiceId; }

    public String getEventSessionId() { return eventSessionId; }
    public void setEventSessionId(String eventSessionId) { this.eventSessionId = eventSessionId; }

    public long getEventOperationId() { return eventOperationId; }
    public void setEventOperationId(long eventOperationId) { this.eventOperationId = eventOperationId; }

    public long getEventMessageId() { return eventMessageId; }
    public void setEventMessageId(long eventMessageId) { this.eventMessageId = eventMessageId; }

    public String getEventCorrelationId() { return eventCorrelationId; }
    public void setEventCorrelationId(String eventCorrelationId) { this.eventCorrelationId = eventCorrelationId; }

    public Instant getEventTimeStamp() { return eventTimeStamp; }
    public void setEventTimeStamp(Instant eventTimeStamp) { this.eventTimeStamp = eventTimeStamp; }
}
