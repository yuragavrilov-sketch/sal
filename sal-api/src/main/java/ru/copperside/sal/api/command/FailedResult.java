package ru.copperside.sal.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result wrapping an exception.
 * <p>
 * C# origin: {@code TCB.Infrastructure.Command.FailedResult}
 * Note: original C# field name is "Exeption" (typo preserved for wire-compat).
 */
public class FailedResult implements CommandResult {

    @JsonProperty("Exeption")
    private String exeption;

    public String getExeption() { return exeption; }
    public void setExeption(String exeption) { this.exeption = exeption; }
}
