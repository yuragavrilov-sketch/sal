package ru.copperside.sal.api.watchdog;

import java.net.URI;
import java.time.Instant;

/** C# origin: {@code TCB.SAL.Common.WatchDog.EndPointInfo} */
public class EndPointInfo {

    private long id;
    private String name;
    private URI serverUri;
    private String hostIp;
    private String environmentKey;
    private boolean pingSucceed;
    private boolean online;
    private int salVersion;
    private long latency;
    private boolean available;
    private long checkFailureCount;
    private Instant lastPresentCheck;
    private long processingRequestCount;
    private long useCount;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public URI getServerUri() { return serverUri; }
    public void setServerUri(URI serverUri) { this.serverUri = serverUri; }
    public String getHostIp() { return hostIp; }
    public void setHostIp(String hostIp) { this.hostIp = hostIp; }
    public String getEnvironmentKey() { return environmentKey; }
    public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
    public boolean isPingSucceed() { return pingSucceed; }
    public void setPingSucceed(boolean pingSucceed) { this.pingSucceed = pingSucceed; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public int getSalVersion() { return salVersion; }
    public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
    public long getLatency() { return latency; }
    public void setLatency(long latency) { this.latency = latency; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public long getCheckFailureCount() { return checkFailureCount; }
    public void setCheckFailureCount(long checkFailureCount) { this.checkFailureCount = checkFailureCount; }
    public Instant getLastPresentCheck() { return lastPresentCheck; }
    public void setLastPresentCheck(Instant lastPresentCheck) { this.lastPresentCheck = lastPresentCheck; }
    public long getProcessingRequestCount() { return processingRequestCount; }
    public void setProcessingRequestCount(long processingRequestCount) { this.processingRequestCount = processingRequestCount; }
    public long getUseCount() { return useCount; }
    public void setUseCount(long useCount) { this.useCount = useCount; }
}
