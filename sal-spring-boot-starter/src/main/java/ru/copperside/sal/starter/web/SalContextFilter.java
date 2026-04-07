package ru.copperside.sal.starter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.context.SalContext;

import java.io.IOException;
import java.util.UUID;

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

        SalContext.setMdc(correlationId, SalContext.sessionId(), properties.getAdapter().getName());
        try {
            filterChain.doFilter(request, response);
        } finally {
            SalContext.clear();
        }
    }
}
