package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;

@ServiceMessage
public class MonitorTimeOutEvent implements Event {
    private String adapterName;
    private String monitorName;

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }
}
