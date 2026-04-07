package ru.copperside.sal.api.event.endpoint;

public class AdapterAvailableEvent extends EndPointEvent {
    private String adapterName;
    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
}
