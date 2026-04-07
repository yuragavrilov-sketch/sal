package ru.copperside.sal.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;

/**
 * Metadata carried with every command through the bus.
 * <p>
 * Wire-critical: field names must match C# PascalCase JSON exactly,
 * including the typo "ExcutionServiceId" (not "Execution").
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.CommandContext}
 */
public class CommandContext {

    private String commandType;
    private String correlationId;
    private String sourceServiceId;
    private Instant timeStamp;
    private CommandPriority priority;
    private Instant expireDate;

    @JsonProperty("ExcutionServiceId")
    private String excutionServiceId;

    @JsonProperty("ExcutionTimeStamp")
    private Instant excutionTimeStamp;

    @JsonProperty("ExcutionDuration")
    private Duration excutionDuration;

    private String sessionId;
    private String operationId;

    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSourceServiceId() { return sourceServiceId; }
    public void setSourceServiceId(String sourceServiceId) { this.sourceServiceId = sourceServiceId; }

    public Instant getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Instant timeStamp) { this.timeStamp = timeStamp; }

    public CommandPriority getPriority() { return priority; }
    public void setPriority(CommandPriority priority) { this.priority = priority; }

    public Instant getExpireDate() { return expireDate; }
    public void setExpireDate(Instant expireDate) { this.expireDate = expireDate; }

    public String getExcutionServiceId() { return excutionServiceId; }
    public void setExcutionServiceId(String excutionServiceId) { this.excutionServiceId = excutionServiceId; }

    public Instant getExcutionTimeStamp() { return excutionTimeStamp; }
    public void setExcutionTimeStamp(Instant excutionTimeStamp) { this.excutionTimeStamp = excutionTimeStamp; }

    public Duration getExcutionDuration() { return excutionDuration; }
    public void setExcutionDuration(Duration excutionDuration) { this.excutionDuration = excutionDuration; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
}
