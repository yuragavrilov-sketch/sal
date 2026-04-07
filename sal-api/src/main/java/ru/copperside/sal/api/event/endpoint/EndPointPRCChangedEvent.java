package ru.copperside.sal.api.event.endpoint;

public class EndPointPRCChangedEvent extends EndPointEvent {
    private long processingRequestCount;

    public long getProcessingRequestCount() { return processingRequestCount; }
    public void setProcessingRequestCount(long processingRequestCount) { this.processingRequestCount = processingRequestCount; }
}
