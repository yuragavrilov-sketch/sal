package ru.copperside.sal.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties mapped from C# JSON configs → application.yml.
 * <p>
 * Covers: Adapter, Service, SalClient, CommandProcessor, EventProcessor sections.
 */
@ConfigurationProperties(prefix = "sal")
public class SalProperties {

    private final Adapter adapter = new Adapter();
    private final Service service = new Service();
    private final Client client = new Client();
    private final Command command = new Command();
    private final Event event = new Event();
    private final WatchDog watchdog = new WatchDog();

    public Adapter getAdapter() { return adapter; }
    public Service getService() { return service; }
    public Client getClient() { return client; }
    public Command getCommand() { return command; }
    public Event getEvent() { return event; }
    public WatchDog getWatchdog() { return watchdog; }

    /** Adapter identity — maps to C# Adapter section. */
    public static class Adapter {
        private String name = "unnamed-adapter";
        private String type = "GenericAdapter";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /** Service parameters — maps to C# Service section. */
    public static class Service {
        private String environmentKey = "";
        private boolean enableOfflineMode = false;
        private boolean mbMode = false;
        private int salVersion = 1;
        private int minEpSalVersion = 0;
        private int maxEpSalVersion = Integer.MAX_VALUE;
        private String[] adapterDependency = new String[0];
        private String dataPath;
        private String diskStorePath;

        public String getEnvironmentKey() { return environmentKey; }
        public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
        public boolean isEnableOfflineMode() { return enableOfflineMode; }
        public void setEnableOfflineMode(boolean enableOfflineMode) { this.enableOfflineMode = enableOfflineMode; }
        public boolean isMbMode() { return mbMode; }
        public void setMbMode(boolean mbMode) { this.mbMode = mbMode; }
        public int getSalVersion() { return salVersion; }
        public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
        public int getMinEpSalVersion() { return minEpSalVersion; }
        public void setMinEpSalVersion(int minEpSalVersion) { this.minEpSalVersion = minEpSalVersion; }
        public int getMaxEpSalVersion() { return maxEpSalVersion; }
        public void setMaxEpSalVersion(int maxEpSalVersion) { this.maxEpSalVersion = maxEpSalVersion; }
        public String[] getAdapterDependency() { return adapterDependency; }
        public void setAdapterDependency(String[] adapterDependency) { this.adapterDependency = adapterDependency; }
        public String getDataPath() { return dataPath; }
        public void setDataPath(String dataPath) { this.dataPath = dataPath; }
        public String getDiskStorePath() { return diskStorePath; }
        public void setDiskStorePath(String diskStorePath) { this.diskStorePath = diskStorePath; }
    }

    /** HTTP client settings — maps to C# SalClient section. */
    public static class Client {
        private int requestTimeout = 120;
        private int serviceRequestTimeout = 10;

        public int getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
        public int getServiceRequestTimeout() { return serviceRequestTimeout; }
        public void setServiceRequestTimeout(int serviceRequestTimeout) { this.serviceRequestTimeout = serviceRequestTimeout; }
    }

    /** Command processor settings — maps to C# CommandProcessor section. */
    public static class Command {
        private int threads = 10;
        private int resultThreads = 10;

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
        public int getResultThreads() { return resultThreads; }
        public void setResultThreads(int resultThreads) { this.resultThreads = resultThreads; }
    }

    /** Event processor settings — maps to C# EventProcessor section. */
    public static class Event {
        private int threads = 1;

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
    }

    /** WatchDog scheduling settings. */
    public static class WatchDog {
        private long pingIntervalMs = 10_000;
        private long removerIntervalMs = 60_000;

        public long getPingIntervalMs() { return pingIntervalMs; }
        public void setPingIntervalMs(long pingIntervalMs) { this.pingIntervalMs = pingIntervalMs; }
        public long getRemoverIntervalMs() { return removerIntervalMs; }
        public void setRemoverIntervalMs(long removerIntervalMs) { this.removerIntervalMs = removerIntervalMs; }
    }
}
