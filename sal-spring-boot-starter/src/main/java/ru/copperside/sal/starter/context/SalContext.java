package ru.copperside.sal.starter.context;

import org.slf4j.MDC;
import ru.copperside.sal.api.command.CommandContext;

import java.util.Map;

/**
 * Unified thread-local context for SAL request processing.
 * Replaces SessionHolder + CommandContextHolder + SalMdc.
 */
public final class SalContext {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_SESSION_ID = "sessionId";
    public static final String MDC_ADAPTER_NAME = "adapterName";

    private static final ThreadLocal<Map<String, Object>> SESSION = new ThreadLocal<>();
    private static final ThreadLocal<CommandContext> COMMAND_CONTEXT = new ThreadLocal<>();

    private SalContext() {}

    // --- Session ---

    public static Map<String, Object> session() {
        return SESSION.get();
    }

    public static void setSession(Map<String, Object> session) {
        SESSION.set(session);
    }

    public static String sessionId() {
        Map<String, Object> session = SESSION.get();
        if (session == null) return null;
        Object sid = session.get("SessionId");
        return sid != null ? sid.toString() : null;
    }

    public static String operationId() {
        Map<String, Object> session = SESSION.get();
        if (session == null) return null;
        Object oid = session.get("OperationId");
        return oid != null ? oid.toString() : null;
    }

    // --- Command Context ---

    public static CommandContext commandContext() {
        return COMMAND_CONTEXT.get();
    }

    public static void setCommandContext(CommandContext ctx) {
        COMMAND_CONTEXT.set(ctx);
    }

    // --- MDC ---

    public static void setMdc(String correlationId, String sessionId, String adapterName) {
        if (correlationId != null) MDC.put(MDC_CORRELATION_ID, correlationId);
        if (sessionId != null) MDC.put(MDC_SESSION_ID, sessionId);
        if (adapterName != null) MDC.put(MDC_ADAPTER_NAME, adapterName);
    }

    // --- Lifecycle ---

    public static void clear() {
        SESSION.remove();
        COMMAND_CONTEXT.remove();
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_ADAPTER_NAME);
    }
}
