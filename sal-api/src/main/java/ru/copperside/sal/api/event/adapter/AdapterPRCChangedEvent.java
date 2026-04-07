package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;

@ServiceMessage
public class AdapterPRCChangedEvent implements Event {
    private String adapterName;
    private long processingRequestCount;

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public long getProcessingRequestCount() { return processingRequestCount; }
    public void setProcessingRequestCount(long processingRequestCount) { this.processingRequestCount = processingRequestCount; }
}
