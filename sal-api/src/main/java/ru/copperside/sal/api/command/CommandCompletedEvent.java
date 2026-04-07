package ru.copperside.sal.api.command;

import java.util.HashMap;
import java.util.Map;

/**
 * Event sent through RabbitMQ when a command completes successfully.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.CommandCompletedEvent}
 */
public class CommandCompletedEvent implements CommandResult {

    private String resultType;
    private Object result;
    private Map<String, String> additionalData = new HashMap<>();
    private CommandContext context;

    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public Map<String, String> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, String> additionalData) { this.additionalData = additionalData; }

    public CommandContext getContext() { return context; }
    public void setContext(CommandContext context) { this.context = context; }
}
