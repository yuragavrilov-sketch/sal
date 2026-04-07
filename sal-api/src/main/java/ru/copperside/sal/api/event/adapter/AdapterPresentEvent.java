package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.dto.service.HostedRequest;
import ru.copperside.sal.api.event.Event;

@ServiceMessage
public class AdapterPresentEvent implements Event {
    private String adapterName;
    private String uri;
    private String environmentKey;
    private int salVersion;
    private HostedRequest[] hostedRequests;
    private boolean mbMode;

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getEnvironmentKey() { return environmentKey; }
    public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
    public int getSalVersion() { return salVersion; }
    public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
    public HostedRequest[] getHostedRequests() { return hostedRequests; }
    public void setHostedRequests(HostedRequest[] hostedRequests) { this.hostedRequests = hostedRequests; }
    public boolean isMbMode() { return mbMode; }
    public void setMbMode(boolean mbMode) { this.mbMode = mbMode; }
}
