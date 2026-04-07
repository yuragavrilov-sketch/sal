package ru.copperside.sal.starter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.copperside.sal.api.exception.ErrorException;
import ru.copperside.sal.api.exception.ExceptionSourceType;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.starter.SalProperties;

/**
 * Rejects requests when the adapter is offline or shutting down.
 * <p>
 * C# origin: {@code CheckOfflineMiddleware}
 */
public class OfflineCheckInterceptor implements HandlerInterceptor {

    private final AdapterState adapterState;
    private final SalProperties properties;

    public OfflineCheckInterceptor(AdapterState adapterState, SalProperties properties) {
        this.adapterState = adapterState;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (adapterState.isShutDown()) {
            throw offlineException();
        }
        boolean offlineModeEnabled = properties.getService().isEnableOfflineMode();
        if (!offlineModeEnabled && !adapterState.isOnline()) {
            throw offlineException();
        }
        return true;
    }

    private ErrorException offlineException() {
        ErrorException ex = new ErrorException(SalErrorCodes.ADAPTER_IS_OFFLINE);
        ex.setCode(SalErrorCodes.ADAPTER_IS_OFFLINE);
        ex.setSourceType(ExceptionSourceType.SAL);
        return ex;
    }
}
