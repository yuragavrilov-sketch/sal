package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;

@ServiceMessage
public class EnvironmentKeyChangedEvent implements Event {
    private String adapterName;
    private String environmentKey;

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getEnvironmentKey() { return environmentKey; }
    public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
}
