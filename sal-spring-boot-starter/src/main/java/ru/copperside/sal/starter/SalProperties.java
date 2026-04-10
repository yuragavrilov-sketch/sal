package ru.copperside.sal.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sal")
public class SalProperties {

    private final Adapter adapter = new Adapter();
    private final Service service = new Service();
    private final Command command = new Command();

    public Adapter getAdapter() { return adapter; }
    public Service getService() { return service; }
    public Command getCommand() { return command; }

    public static class Adapter {
        private String name = "unnamed-adapter";
        private String type = "GenericAdapter";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Service {
        private String environmentKey = "";
        private int salVersion = 1;

        public String getEnvironmentKey() { return environmentKey; }
        public void setEnvironmentKey(String environmentKey) { this.environmentKey = environmentKey; }
        public int getSalVersion() { return salVersion; }
        public void setSalVersion(int salVersion) { this.salVersion = salVersion; }
    }

    public static class Command {
        private int threads = 10;
        private int resultThreads = 10;

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
        public int getResultThreads() { return resultThreads; }
        public void setResultThreads(int resultThreads) { this.resultThreads = resultThreads; }
    }
}
