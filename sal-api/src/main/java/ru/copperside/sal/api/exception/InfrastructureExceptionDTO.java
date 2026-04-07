package ru.copperside.sal.api.exception;

import java.time.Instant;

/**
 * Wire-format DTO for exceptions transmitted between adapters.
 * <p>
 * Wire-critical: PascalCase JSON, recursive InnerException.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Exceptions.InfrastructureExceptionDTO}
 */
public class InfrastructureExceptionDTO {

    private String exceptionType;
    private String code;
    private String codeDescription;
    private String message;
    private String adapterName;
    private String sourceType;
    private String sourcePath;
    private String sessionId;
    private String sourceId;
    private Instant timeStamp;
    private Object properties;
    private InfrastructureExceptionDTO innerException;

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCodeDescription() { return codeDescription; }
    public void setCodeDescription(String codeDescription) { this.codeDescription = codeDescription; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public Instant getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Instant timeStamp) { this.timeStamp = timeStamp; }

    public Object getProperties() { return properties; }
    public void setProperties(Object properties) { this.properties = properties; }

    public InfrastructureExceptionDTO getInnerException() { return innerException; }
    public void setInnerException(InfrastructureExceptionDTO innerException) { this.innerException = innerException; }
}
