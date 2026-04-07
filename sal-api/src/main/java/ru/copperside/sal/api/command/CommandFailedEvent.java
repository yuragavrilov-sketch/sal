package ru.copperside.sal.api.command;

import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Event sent through RabbitMQ when a command fails.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.CommandFailedEvent}
 */
public class CommandFailedEvent {

    private InfrastructureExceptionDTO exceptionData;
    private Map<String, String> additionalData = new HashMap<>();
    private CommandContext context;

    public InfrastructureExceptionDTO getExceptionData() { return exceptionData; }
    public void setExceptionData(InfrastructureExceptionDTO exceptionData) { this.exceptionData = exceptionData; }

    public Map<String, String> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, String> additionalData) { this.additionalData = additionalData; }

    public CommandContext getContext() { return context; }
    public void setContext(CommandContext context) { this.context = context; }
}
