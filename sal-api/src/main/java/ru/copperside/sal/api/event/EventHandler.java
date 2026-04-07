package ru.copperside.sal.api.event;

/**
 * Handles domain events.
 * <p>
 * C# origin: {@code IEventHandle{TEvent}}
 *
 * @param <E> event type
 */
public interface EventHandler<E extends Event> {

    void handle(E event, EventSource source);
}
