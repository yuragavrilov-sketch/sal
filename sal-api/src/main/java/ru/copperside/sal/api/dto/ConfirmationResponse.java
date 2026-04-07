package ru.copperside.sal.api.dto;

/** C# origin: {@code TCB.SAL.Client.ConfirmationResponse} */
public class ConfirmationResponse {

    private boolean confirmationRequired;
    private Object data;
    private String dataType;

    public boolean isConfirmationRequired() { return confirmationRequired; }
    public void setConfirmationRequired(boolean confirmationRequired) { this.confirmationRequired = confirmationRequired; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
}
