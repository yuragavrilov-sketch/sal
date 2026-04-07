package ru.copperside.sal.api.event.adapter;

import ru.copperside.sal.api.event.Event;
import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;

/** C# origin: {@code TCB.SAL.Events.ExceptionDetectEvent} */
public class ExceptionDetectEvent implements Event {
    private String adapterName;
    private InfrastructureExceptionDTO exceptionDto;

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public InfrastructureExceptionDTO getExceptionDto() { return exceptionDto; }
    public void setExceptionDto(InfrastructureExceptionDTO exceptionDto) { this.exceptionDto = exceptionDto; }
}
