package ru.copperside.sal.api.exception;

/** C# origin: {@code TCB.SAL.Client.Exceptions.ValidationException} */
public class ValidationException extends SalBaseException {
    public ValidationException() { super(); }
    public ValidationException(String message) { super(message); }
    public ValidationException(InfrastructureExceptionDTO data) { super(data); }
    public ValidationException(InfrastructureExceptionDTO data, Throwable cause) { super(data, cause); }
}
