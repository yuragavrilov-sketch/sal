package ru.copperside.sal.api.exception;

import java.time.Instant;

public class SalException extends RuntimeException {

    public enum Type { ERROR, FATAL, VALIDATION }

    private final Type type;
    private String code;
    private String codeDescription;
    private String adapterName;
    private String sourceType;
    private String sourcePath;
    private String sessionId;
    private String sourceId;
    private Instant timeStamp;
    private Object properties;

    private SalException(Type type, String message) {
        super(message);
        this.type = type;
    }

    private SalException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    private SalException(Type type, InfrastructureExceptionDTO data) {
        super(data.getMessage());
        this.type = type;
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

    public static SalException error(String message) {
        return new SalException(Type.ERROR, message);
    }

    public static SalException error(String code, String message) {
        SalException ex = new SalException(Type.ERROR, message);
        ex.setCode(code);
        return ex;
    }

    public static SalException fatal(String message) {
        return new SalException(Type.FATAL, message);
    }

    public static SalException validation(String message) {
        return new SalException(Type.VALIDATION, message);
    }

    public static SalException fromDto(Type type, InfrastructureExceptionDTO data) {
        return new SalException(type, data);
    }

    /**
     * Returns the wire-format exception type name (e.g. "ErrorException", "FatalException").
     */
    public String getExceptionTypeName() {
        return switch (type) {
            case ERROR -> "ErrorException";
            case FATAL -> "FatalException";
            case VALIDATION -> "ValidationException";
        };
    }

    public Type getType() { return type; }

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
