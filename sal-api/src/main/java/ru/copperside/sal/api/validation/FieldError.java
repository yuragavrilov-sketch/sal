package ru.copperside.sal.api.validation;

/** C# origin: {@code TCB.Infrastructure.Validation.FieldError} */
public class FieldError {

    private String fieldName;
    private String path;
    private String errorCode;
    private String description;

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
