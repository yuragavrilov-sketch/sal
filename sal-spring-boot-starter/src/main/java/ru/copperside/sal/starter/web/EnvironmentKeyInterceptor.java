package ru.copperside.sal.starter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.api.exception.ErrorException;
import ru.copperside.sal.api.exception.ExceptionSourceType;
import ru.copperside.sal.api.exception.SalErrorCodes;
import ru.copperside.sal.starter.SalProperties;

/**
 * Validates the {@code TCB.Header-EnvironmentKey} header on each request.
 * <p>
 * C# origin: {@code CheckEnvironmentKeyMiddleware}
 */
public class EnvironmentKeyInterceptor implements HandlerInterceptor {

    private final SalProperties properties;

    public EnvironmentKeyInterceptor(SalProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String expectedKey = properties.getService().getEnvironmentKey();
        // If no key configured, skip validation
        if (expectedKey == null || expectedKey.isBlank()) {
            return true;
        }
        String receivedKey = request.getHeader(Headers.ENVIRONMENT_KEY);
        if (!expectedKey.equals(receivedKey)) {
            ErrorException ex = new ErrorException(SalErrorCodes.MISMATCH_ENVIRONMENT_KEY);
            ex.setCode(SalErrorCodes.MISMATCH_ENVIRONMENT_KEY);
            ex.setSourceType(ExceptionSourceType.SAL);
            throw ex;
        }
        return true;
    }
}
