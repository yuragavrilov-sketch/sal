package ru.copperside.sal.starter.context;

import org.slf4j.MDC;

/**
 * MDC utility for SAL correlation fields (ADR-006).
 * <p>
 * Sets/clears MDC keys used in logback-spring.xml: correlationId, sessionId, adapterName.
 */
public final class SalMdc {

    public static final String CORRELATION_ID = "correlationId";
    public static final String SESSION_ID = "sessionId";
    public static final String ADAPTER_NAME = "adapterName";

    private SalMdc() {}

    public static void set(String correlationId, String sessionId, String adapterName) {
        if (correlationId != null) MDC.put(CORRELATION_ID, correlationId);
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
        if (adapterName != null) MDC.put(ADAPTER_NAME, adapterName);
    }

    public static void clear() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(SESSION_ID);
        MDC.remove(ADAPTER_NAME);
    }
}
