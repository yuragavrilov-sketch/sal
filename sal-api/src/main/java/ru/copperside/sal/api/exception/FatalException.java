package ru.copperside.sal.api.exception;

/** C# origin: {@code TCB.SAL.Client.Exceptions.FatalException} */
public class FatalException extends SalBaseException {
    public FatalException() { super(); }
    public FatalException(String message) { super(message); }
    public FatalException(String message, Throwable cause) { super(message, cause); }
    public FatalException(InfrastructureExceptionDTO data) { super(data); }
    public FatalException(InfrastructureExceptionDTO data, Throwable cause) { super(data, cause); }
}
