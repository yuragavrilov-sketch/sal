package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;

@ServiceMessage
public class MonitorFailureEvent implements Event {
    private String adapterName;
    private String monitorName;
    private MonitorException exeption; // typo preserved from C#

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }
    public MonitorException getExeption() { return exeption; }
    public void setExeption(MonitorException exeption) { this.exeption = exeption; }

    public static class MonitorException {
        private String message;
        private String type;
        private Object propertes; // typo preserved from C#
        private MonitorException inerException; // typo preserved from C#

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getPropertes() { return propertes; }
        public void setPropertes(Object propertes) { this.propertes = propertes; }
        public MonitorException getInerException() { return inerException; }
        public void setInerException(MonitorException inerException) { this.inerException = inerException; }
    }
}
