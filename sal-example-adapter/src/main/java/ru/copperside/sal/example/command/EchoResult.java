package ru.copperside.sal.example.command;

import ru.copperside.sal.api.command.CommandResult;

/**
 * Result for {@link EchoCommand}.
 */
public class EchoResult implements CommandResult {

    private String echo;

    public String getEcho() { return echo; }
    public void setEcho(String echo) { this.echo = echo; }
}
