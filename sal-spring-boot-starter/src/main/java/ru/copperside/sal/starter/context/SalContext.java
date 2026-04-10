package ru.copperside.sal.starter.context;

import org.slf4j.MDC;
import ru.copperside.sal.api.command.CommandContext;

import java.util.Map;

/**
 * Unified thread-local context for SAL command processing.
 */
public final class SalContext {

    public static final String MDC_CORRELATION_ID = "correlationId";

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

    // --- Command Context ---

    public static CommandContext commandContext() {
        return COMMAND_CONTEXT.get();
    }

    public static void setCommandContext(CommandContext ctx) {
        COMMAND_CONTEXT.set(ctx);
    }

    // --- MDC ---

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) MDC.put(MDC_CORRELATION_ID, correlationId);
    }

    // --- Lifecycle ---

    public static void clear() {
        SESSION.remove();
        COMMAND_CONTEXT.remove();
        MDC.remove(MDC_CORRELATION_ID);
    }
}
