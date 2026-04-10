package ru.copperside.sal.api.exception;

/**
 * Unified SAL exception. Use factory methods {@link #error}, {@link #fatal},
 * {@link #validation} to create instances of each type.
 */
public class SalException extends RuntimeException {

    public enum Type { ERROR, FATAL, VALIDATION }

    private final Type type;
    private String code;

    private SalException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public static SalException error(String message) {
        return new SalException(Type.ERROR, message);
    }

    public static SalException error(String code, String message) {
        SalException ex = new SalException(Type.ERROR, message);
        ex.code = code;
        return ex;
    }

    public static SalException fatal(String message) {
        return new SalException(Type.FATAL, message);
    }

    public static SalException validation(String message) {
        return new SalException(Type.VALIDATION, message);
    }

    /**
     * Wire-format exception type name for C# interop
     * (e.g. "ErrorException", "FatalException", "ValidationException").
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
}
