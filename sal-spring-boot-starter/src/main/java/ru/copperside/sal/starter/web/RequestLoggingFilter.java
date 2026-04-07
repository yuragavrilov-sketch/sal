package ru.copperside.sal.starter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.copperside.sal.api.constant.Headers;

import java.io.IOException;

/**
 * Logs each HTTP request with path, method, remote IP, status code, and elapsed time.
 * <p>
 * C# origin: {@code LogMiddleware}
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("IncomingLogger");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);

        boolean isConfirmation = request.getHeader(Headers.CHECK_CONFIRMATION) != null;
        String marker = isConfirmation ? " [C]" : "";

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.debug("{} {}{} from {} -> {} ({}ms)", method, path, marker, clientIp, status, elapsed);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
