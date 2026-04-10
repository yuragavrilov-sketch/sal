package ru.copperside.sal.starter.command;

import org.junit.jupiter.api.Test;
import ru.copperside.sal.api.command.CommandPriority;
import ru.copperside.sal.starter.context.SalContext;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DefaultCommandBus pending command management and context holders.
 */
class DefaultCommandBusTest {

    @Test
    void completePendingCommand_shouldResolveFuture() throws Exception {
        var pendingCommands = new ConcurrentHashMap<String, PendingCommand>();
        var bus = new DefaultCommandBus(null, null);

        var future = new java.util.concurrent.CompletableFuture<Object>();
        pendingCommands.put("abc-123", new PendingCommand("abc-123", Instant.now().plusSeconds(120), future));
        bus.pendingCommands.putAll(pendingCommands);

        boolean consumed = bus.completePendingCommand("abc-123", "result-value");
        assertThat(consumed).isTrue();
        assertThat(future.get(1, TimeUnit.SECONDS)).isEqualTo("result-value");
        assertThat(bus.pendingCommands).isEmpty();
    }

    @Test
    void failPendingCommand_shouldRejectFuture() {
        var bus = new DefaultCommandBus(null, null);
        var future = new java.util.concurrent.CompletableFuture<Object>();
        bus.pendingCommands.put("xyz-789", new PendingCommand("xyz-789", Instant.now().plusSeconds(120), future));

        boolean consumed = bus.failPendingCommand("xyz-789", new RuntimeException("test error"));
        assertThat(consumed).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
    }

    @Test
    void completePendingCommand_shouldReturnFalseForUnknownCorrelationId() {
        var bus = new DefaultCommandBus(null, null);
        boolean consumed = bus.completePendingCommand("unknown-id", "value");
        assertThat(consumed).isFalse();
    }

    @Test
    void commandTimeoutWatcher_shouldExpireOldCommands() {
        var pendingCommands = new ConcurrentHashMap<String, PendingCommand>();
        var future = new java.util.concurrent.CompletableFuture<Object>();

        // Expired 10 seconds ago
        pendingCommands.put("expired", new PendingCommand("expired", Instant.now().minusSeconds(10), future));

        var watcher = new CommandTimeoutWatcher(pendingCommands);
        watcher.checkTimeouts();

        assertThat(pendingCommands).isEmpty();
        assertThat(future.isCompletedExceptionally()).isTrue();
    }

    @Test
    void salContext_session_shouldBeThreadLocal() {
        SalContext.setSession(Map.of("SessionId", "test-123", "OperationId", 42));
        assertThat(SalContext.session()).containsEntry("SessionId", "test-123");

        SalContext.clear();
        assertThat(SalContext.session()).isNull();
    }

    @Test
    void salContext_commandContext_shouldBeThreadLocal() {
        var ctx = new ru.copperside.sal.api.command.CommandContext();
        ctx.setCorrelationId("test-cid");
        SalContext.setCommandContext(ctx);

        assertThat(SalContext.commandContext().getCorrelationId()).isEqualTo("test-cid");

        SalContext.clear();
        assertThat(SalContext.commandContext()).isNull();
    }

    @Test
    void salContext_mdc_shouldSetAndClear() {
        SalContext.setCorrelationId("cid-1");
        assertThat(org.slf4j.MDC.get("correlationId")).isEqualTo("cid-1");

        SalContext.clear();
        assertThat(org.slf4j.MDC.get("correlationId")).isNull();
    }

    @Test
    void commandPriority_shouldMapToByteValues() {
        assertThat((byte) CommandPriority.Idle.getValue()).isEqualTo((byte) 0);
        assertThat((byte) CommandPriority.Normal.getValue()).isEqualTo((byte) 5);
        assertThat((byte) CommandPriority.RealTime.getValue()).isEqualTo((byte) 10);
    }
}
