package ru.copperside.sal.starter.context;

import java.util.Map;

/**
 * Thread-local holder for session data (ADR-010).
 * <p>
 * Replaces C# {@code SessionManager} + {@code CallContext.LogicalSetData("Session")}.
 * Must be cleared in finally-blocks of every entry point (HTTP interceptor, RabbitMQ consumer).
 */
public final class SessionHolder {

    private static final ThreadLocal<Map<String, Object>> CURRENT = new ThreadLocal<>();

    private SessionHolder() {}

    public static Map<String, Object> get() {
        return CURRENT.get();
    }

    public static void set(Map<String, Object> session) {
        CURRENT.set(session);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static String getSessionId() {
        Map<String, Object> session = CURRENT.get();
        if (session == null) return null;
        Object sid = session.get("SessionId");
        return sid != null ? sid.toString() : null;
    }

    public static String getOperationId() {
        Map<String, Object> session = CURRENT.get();
        if (session == null) return null;
        Object oid = session.get("OperationId");
        return oid != null ? oid.toString() : null;
    }
}
