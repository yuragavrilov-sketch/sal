package ru.copperside.sal.starter.command;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks a pending command awaiting result (replaces C# SimpleCommandResultHandlerInfo).
 */
record PendingCommand(
        String correlationId,
        Instant expireDate,
        CompletableFuture<Object> future
) {
}
