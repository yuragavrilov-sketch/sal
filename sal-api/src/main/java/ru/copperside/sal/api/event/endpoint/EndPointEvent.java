package ru.copperside.sal.api.event.endpoint;

import ru.copperside.sal.api.annotation.ServiceMessage;
import ru.copperside.sal.api.event.Event;

/** Base class for endpoint events. C# origin: {@code TCB.EndPoint.Client.EndPointEvent} */
@ServiceMessage
public class EndPointEvent implements Event {
    private String endPointName;
    private String endPointUri;

    public String getEndPointName() { return endPointName; }
    public void setEndPointName(String endPointName) { this.endPointName = endPointName; }
    public String getEndPointUri() { return endPointUri; }
    public void setEndPointUri(String endPointUri) { this.endPointUri = endPointUri; }
}
