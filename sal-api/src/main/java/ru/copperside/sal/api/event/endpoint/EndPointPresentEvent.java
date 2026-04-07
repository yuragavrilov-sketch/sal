package ru.copperside.sal.api.event.endpoint;

public class EndPointPresentEvent extends EndPointEvent {
    private String environmentKey;
    private int salVersion;

    public String getEnvironmentKey() { return environmentKey; }
    public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
    public int getSalVersion() { return salVersion; }
    public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
}
