package ru.copperside.sal.api.command;

/**
 * Result of a two-phase confirmation check.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.ConfirmationResult}
 */
public class ConfirmationResult implements CommandResult {

    private boolean confirmationRequired;
    private String confirmationDataType;
    private Object confirmationData;

    public boolean isConfirmationRequired() { return confirmationRequired; }
    public void setConfirmationRequired(boolean confirmationRequired) { this.confirmationRequired = confirmationRequired; }

    public String getConfirmationDataType() { return confirmationDataType; }
    public void setConfirmationDataType(String confirmationDataType) { this.confirmationDataType = confirmationDataType; }

    public Object getConfirmationData() { return confirmationData; }
    public void setConfirmationData(Object confirmationData) { this.confirmationData = confirmationData; }
}
