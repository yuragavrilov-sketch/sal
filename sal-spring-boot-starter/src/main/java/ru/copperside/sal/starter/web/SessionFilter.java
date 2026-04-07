package ru.copperside.sal.starter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.starter.context.SalContext;
import ru.copperside.sal.starter.session.SessionSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Extracts and restores session from the HTTP body using the C# wire protocol.
 * <p>
 * Wire protocol (C# {@code SessionDataMiddleware}):
 * <pre>
 *   Request header: {@code TCB.Header-Session: {offset};{length}}
 *   Request body:   [actual_data bytes 0..offset] + [session_bytes offset..offset+length]
 *
 *   Response header: {@code TCB.Header-Session: {responseBodyLen};{sessionLen}}
 *   Response body:  [actual_response] + [session_bytes]
 * </pre>
 * After the request completes, if session is set, it is appended to the response body.
 */
public class SessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionFilter.class);

    private final SessionSerializer sessionSerializer;

    public SessionFilter(SessionSerializer sessionSerializer) {
        this.sessionSerializer = sessionSerializer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String sessionHeader = request.getHeader(Headers.SESSION);

        if (sessionHeader == null || sessionHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String[] parts = sessionHeader.split(";");
        if (parts.length != 2) {
            filterChain.doFilter(request, response);
            return;
        }

        int offset;
        int length;
        try {
            offset = Integer.parseInt(parts[0].trim());
            length = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            filterChain.doFilter(request, response);
            return;
        }

        // Buffer full request body
        byte[] fullBody = request.getInputStream().readAllBytes();

        // Extract and set session from body
        if (fullBody.length >= offset + length) {
            try {
                String sessionStr = new String(fullBody, offset, length, StandardCharsets.UTF_8);
                Map<String, Object> session = sessionSerializer.deserialize(sessionStr);
                SalContext.setSession(session);
            } catch (Exception e) {
                log.warn("Failed to deserialize session: {}", e.getMessage());
            }
        }

        // Wrap request with body stripped of session bytes
        byte[] requestBody = offset > 0 ? java.util.Arrays.copyOf(fullBody, offset) : new byte[0];
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, requestBody);

        // Wrap response to capture output for session appending
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            appendSessionToResponse(wrappedResponse, response);
            SalContext.clear();
        }
    }

    private void appendSessionToResponse(ContentCachingResponseWrapper wrappedResponse,
                                         HttpServletResponse response) throws IOException {
        byte[] responseBody = wrappedResponse.getContentAsByteArray();
        Map<String, Object> session = SalContext.session();

        if (session != null && !session.isEmpty()) {
            String sessionStr;
            try {
                sessionStr = sessionSerializer.serialize(session);
            } catch (Exception e) {
                log.warn("Failed to serialize session for response: {}", e.getMessage());
                wrappedResponse.copyBodyToResponse();
                return;
            }
            byte[] sessionBytes = sessionStr.getBytes(StandardCharsets.UTF_8);

            response.setHeader(Headers.SESSION, responseBody.length + ";" + sessionBytes.length);
            response.setContentLength(responseBody.length + sessionBytes.length);
            response.getOutputStream().write(responseBody);
            response.getOutputStream().write(sessionBytes);
        } else {
            wrappedResponse.copyBodyToResponse();
        }
    }

    /** Servlet request wrapper that replaces the body with pre-read bytes. */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            InputStream in = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override public boolean isFinished() { try { return in.available() == 0; } catch (IOException e) { return true; } }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener l) {}
                @Override public int read() throws IOException { return in.read(); }
                @Override public int read(byte[] b, int off, int len) throws IOException { return in.read(b, off, len); }
                @Override public int available() throws IOException { return in.available(); }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
