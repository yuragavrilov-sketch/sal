package ru.copperside.sal.api.exception;

/**
 * Exception type discriminator for wire-format.
 * <p>
 * C# origin: {@code TCB.SAL.Client.Exceptions.ExceptionType}
 */
public final class ExceptionType {

    private ExceptionType() {}

    public static final String ERROR = "Error";
    public static final String VALIDATION = "Validation";
    public static final String FATAL = "Fatal";
}
