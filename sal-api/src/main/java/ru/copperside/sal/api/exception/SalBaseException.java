package ru.copperside.sal.api.exception;

import java.time.Instant;

/**
 * Base exception for all SAL exceptions.
 * <p>
 * C# origin: {@code TCB.SAL.Client.Exceptions.SalBaseException}
 */
public abstract class SalBaseException extends RuntimeException {

    private String code;
    private String codeDescription;
    private String adapterName;
    private String sourceType;
    private String sourcePath;
    private String sessionId;
    private String sourceId;
    private Instant timeStamp;
    private Object properties;

    protected SalBaseException() { super(); }

    protected SalBaseException(String message) { super(message); }

    protected SalBaseException(String message, Throwable cause) { super(message, cause); }

    protected SalBaseException(InfrastructureExceptionDTO data) {
        super(data.getMessage());
        this.code = data.getCode();
        this.codeDescription = data.getCodeDescription();
        this.adapterName = data.getAdapterName();
        this.sourceType = data.getSourceType();
        this.sessionId = data.getSessionId();
        this.sourceId = data.getSourceId();
        this.sourcePath = data.getSourcePath();
        this.timeStamp = data.getTimeStamp();
        this.properties = data.getProperties();
    }

    protected SalBaseException(InfrastructureExceptionDTO data, Throwable cause) {
        super(data.getMessage(), cause);
        this.code = data.getCode();
        this.codeDescription = data.getCodeDescription();
        this.adapterName = data.getAdapterName();
        this.sourceType = data.getSourceType();
        this.sessionId = data.getSessionId();
        this.sourceId = data.getSourceId();
        this.sourcePath = data.getSourcePath();
        this.timeStamp = data.getTimeStamp();
        this.properties = data.getProperties();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCodeDescription() { return codeDescription; }
    public void setCodeDescription(String codeDescription) { this.codeDescription = codeDescription; }

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
}
