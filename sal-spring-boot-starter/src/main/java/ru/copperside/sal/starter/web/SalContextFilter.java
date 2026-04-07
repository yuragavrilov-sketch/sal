package ru.copperside.sal.starter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SalMdc;
import ru.copperside.sal.starter.context.SessionHolder;

import java.io.IOException;
import java.util.UUID;

/**
 * Sets SAL correlation context (MDC) for each HTTP request.
 * <p>
 * Reads correlationId from {@code TCB-Header-OperationId} or generates one.
 * Clears MDC and SessionHolder in finally.
 * <p>
 * C# origin: {@code SetSalHttpContextMiddleware}
 */
public class SalContextFilter extends OncePerRequestFilter {

    private final SalProperties properties;

    public SalContextFilter(SalProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(Headers.OPERATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String sessionId = SessionHolder.getSessionId();
        String adapterName = properties.getAdapter().getName();

        SalMdc.set(correlationId, sessionId, adapterName);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SalMdc.clear();
        }
    }
}
