package ru.copperside.sal.api.event;

import ru.copperside.sal.api.message.RecordedMessage;

/**
 * Event Bus — publish domain events to all subscribers via RabbitMQ.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Event.IEventBus}
 */
public interface EventBus {

    void publish(Event... events);

    void publish(Event event);

    /** Low-level: publish pre-built RecordedMessage. */
    void publish(RecordedMessage message);
}
