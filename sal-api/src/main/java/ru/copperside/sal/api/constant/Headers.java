package ru.copperside.sal.api.constant;

/** HTTP header names used in the SAL protocol. C# origin: {@code TCB.SAL.Client.Const.Headers} */
public final class Headers {

    private Headers() {}

    public static final String SESSION = "TCB.Header-Session";
    public static final String CHECK_CONFIRMATION = "TCB-Header-CheckConfirmation";
    public static final String IGNORE_AP = "TCB-Header-IgnoreAP";
    public static final String ADAPTER_NAME = "TCB-Header-AdapterName";
    public static final String ROUTE_CHAIN = "TCB-Header-RouteChain";
    public static final String CONFIRMATION_CODE = "TCB-Header-ConfirmationCode";
    public static final String CONFIRMATION_CODE_TYPE = "TCB-Header-ConfirmationCodeType";
    public static final String SERIALIZER_TYPE = "TCB-Header-SerializerType";
    public static final String DEVICE_ID = "TCB-Header-DeviceId";
    public static final String OPERATION_ID = "TCB-Header-OperationId";
    public static final String ENVIRONMENT_KEY = "TCB.Header-EnvironmentKey";
    public static final String SECURITY = "TCB-Header-SecurityToken";
    public static final String LOGIN = "TCB-Header-Login";
    public static final String SECRET = "TCB-Header-Secret";
    public static final String SIGN = "TCB-Header-Sign";
    public static final String TIMEOUT = "TCB-Header-Timeout";
    public static final String TIMESTAMP = "TCB-Header-TimeStamp";
    public static final String ORIGIN = "TCB-Header-Origin";
}
