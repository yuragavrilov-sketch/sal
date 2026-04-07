package ru.copperside.sal.example.command;

import ru.copperside.sal.api.annotation.CommandType;
import ru.copperside.sal.api.command.HaveResult;

/**
 * Example command — echoes the payload back as a result.
 * Demonstrates how a client adapter defines its command types.
 */
@CommandType("ru.copperside.sal.example.EchoCommand")
public class EchoCommand implements HaveResult<EchoResult> {

    private String payload;

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
