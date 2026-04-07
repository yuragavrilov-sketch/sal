package ru.copperside.sal.api.dto.service;

/** C# origin: {@code TCB.SAL.Client.Service.DTO.HostedRequest} */
public class HostedRequest {

    private String requestType;
    private String responseType;
    private String localPath;

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
}
