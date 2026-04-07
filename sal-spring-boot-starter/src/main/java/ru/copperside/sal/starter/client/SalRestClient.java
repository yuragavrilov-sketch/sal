package ru.copperside.sal.starter.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.copperside.sal.api.constant.Headers;
import ru.copperside.sal.api.dto.service.PingResponse;
import ru.copperside.sal.starter.context.SessionHolder;
import ru.copperside.sal.starter.session.SessionSerializer;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * RestTemplate-based HTTP client for SAL inter-adapter communication.
 * <p>
 * Handles session propagation via {@code TCB.Header-Session} header
 * and environment key authentication.
 * <p>
 * C# origin: {@code BaseSalClient} + {@code PublicClient} / {@code EndPointClient}.
 */
public class SalRestClient {

    private static final Logger log = LoggerFactory.getLogger(SalRestClient.class);

    private final RestTemplate restTemplate;
    private final SessionSerializer sessionSerializer;
    private final com.fasterxml.jackson.databind.ObjectMapper wireObjectMapper;

    public SalRestClient(RestTemplate restTemplate,
                         SessionSerializer sessionSerializer,
                         com.fasterxml.jackson.databind.ObjectMapper wireObjectMapper) {
        this.restTemplate = restTemplate;
        this.sessionSerializer = sessionSerializer;
        this.wireObjectMapper = wireObjectMapper;
    }

    /**
     * Ping a SAL adapter at the given URI.
     *
     * @param serverUri     base URI of the target adapter
     * @param environmentKey environment key for authentication (may be null)
     * @param senderName    name of the sending adapter (for logging)
     * @return PingResponse
     */
    public PingResponse ping(URI serverUri, String environmentKey, String senderName) {
        URI pingUri = serverUri.resolve("/ping");
        HttpHeaders headers = buildHeaders(environmentKey, null);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<PingResponse> response = restTemplate.exchange(
                pingUri, HttpMethod.GET, entity, PingResponse.class);
        return response.getBody();
    }

    /**
     * POST a request body to a route on a SAL adapter, propagating the current session.
     */
    public <T> T post(URI serverUri, String route, String environmentKey, Object requestBody, Class<T> responseType) {
        URI targetUri = serverUri.resolve(route);

        String sessionStr = serializeCurrentSession();
        byte[] bodyBytes = serializeBody(requestBody);
        byte[] fullBody = appendSession(bodyBytes, sessionStr);

        HttpHeaders headers = buildHeaders(environmentKey, sessionStr != null
                ? bodyBytes.length + ";" + sessionStr.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                : null);
        headers.setContentLength(fullBody.length);

        HttpEntity<byte[]> entity = new HttpEntity<>(fullBody, headers);
        ResponseEntity<T> response = restTemplate.exchange(targetUri, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    private HttpHeaders buildHeaders(String environmentKey, String sessionHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (environmentKey != null && !environmentKey.isBlank()) {
            headers.set(Headers.ENVIRONMENT_KEY, environmentKey);
        }
        if (sessionHeader != null) {
            headers.set(Headers.SESSION, sessionHeader);
        }
        return headers;
    }

    private String serializeCurrentSession() {
        Map<String, Object> session = SessionHolder.get();
        if (session == null || session.isEmpty()) return null;
        try {
            return sessionSerializer.serialize(session);
        } catch (IOException e) {
            log.warn("Failed to serialize session for outgoing request: {}", e.getMessage());
            return null;
        }
    }

    private byte[] serializeBody(Object body) {
        if (body == null) return new byte[0];
        try {
            return wireObjectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private byte[] appendSession(byte[] body, String sessionStr) {
        if (sessionStr == null) return body;
        byte[] sessionBytes = sessionStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = new byte[body.length + sessionBytes.length];
        System.arraycopy(body, 0, combined, 0, body.length);
        System.arraycopy(sessionBytes, 0, combined, body.length, sessionBytes.length);
        return combined;
    }
}
