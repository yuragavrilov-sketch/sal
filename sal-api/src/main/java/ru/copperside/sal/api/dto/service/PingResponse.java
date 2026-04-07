package ru.copperside.sal.api.dto.service;

import java.time.OffsetDateTime;

/** C# origin: {@code TCB.SAL.Client.Service.PingResponse} */
public class PingResponse {

    private String recipientServiceName;
    private boolean online;
    private OffsetDateTime serverTime;
    private Integer salVersion;

    public String getRecipientServiceName() { return recipientServiceName; }
    public void setRecipientServiceName(String recipientServiceName) { this.recipientServiceName = recipientServiceName; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public OffsetDateTime getServerTime() { return serverTime; }
    public void setServerTime(OffsetDateTime serverTime) { this.serverTime = serverTime; }

    public Integer getSalVersion() { return salVersion; }
    public void setSalVersion(Integer salVersion) { this.salVersion = salVersion; }
}
