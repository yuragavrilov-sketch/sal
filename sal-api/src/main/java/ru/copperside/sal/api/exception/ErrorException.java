package ru.copperside.sal.api.exception;

/** C# origin: {@code TCB.SAL.Client.Exceptions.ErrorException} */
public class ErrorException extends SalBaseException {
    public ErrorException() { super(); }
    public ErrorException(String message) { super(message); }
    public ErrorException(String message, Throwable cause) { super(message, cause); }
    public ErrorException(InfrastructureExceptionDTO data) { super(data); }
    public ErrorException(InfrastructureExceptionDTO data, Throwable cause) { super(data, cause); }
}
