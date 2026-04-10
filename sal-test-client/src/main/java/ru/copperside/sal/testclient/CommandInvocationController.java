package ru.copperside.sal.testclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.api.command.CommandPriority;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Generic command invocation endpoint — publishes any command by wire-type
 * name and returns the raw deserialized result.
 */
@RestController
@RequestMapping("/api")
public class CommandInvocationController {

    private static final Logger log = LoggerFactory.getLogger(CommandInvocationController.class);

    private final CommandBus commandBus;

    public CommandInvocationController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> invoke(@RequestBody CommandRequest request) {
        if (request.type == null || request.type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "'type' is required"));
        }

        int timeoutSeconds = request.timeoutSeconds != null && request.timeoutSeconds > 0
                ? request.timeoutSeconds : 30;
        CommandPriority priority = request.priority != null
                ? CommandPriority.valueOf(request.priority) : CommandPriority.Normal;
        Object payload = request.payload != null ? request.payload : Map.of();

        log.info("Invoking command '{}' (timeout={}s, priority={})",
                request.type, timeoutSeconds, priority);

        Instant started = Instant.now();
        CompletableFuture<Object> future = commandBus.executeCommandAsync(
                request.type, payload, timeoutSeconds, priority);

        try {
            Object result = future.get(timeoutSeconds + 5L, TimeUnit.SECONDS);
            long elapsedMs = Duration.between(started, Instant.now()).toMillis();
            return ResponseEntity.ok(response("ok", request.type, elapsedMs, result, null));
        } catch (TimeoutException e) {
            long elapsedMs = Duration.between(started, Instant.now()).toMillis();
            log.warn("Command '{}' timed out after {} ms", request.type, elapsedMs);
            return ResponseEntity.status(504).body(
                    response("timeout", request.type, elapsedMs, null, "Timed out waiting for result"));
        } catch (ExecutionException e) {
            long elapsedMs = Duration.between(started, Instant.now()).toMillis();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Command '{}' failed: {}", request.type, cause.getMessage());
            return ResponseEntity.status(500).body(
                    response("failed", request.type, elapsedMs, null, cause.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(
                    response("interrupted", request.type, 0L, null, e.getMessage()));
        }
    }

    private Map<String, Object> response(String status, String type, long elapsedMs,
                                          Object result, String error) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("type", type);
        map.put("elapsedMs", elapsedMs);
        if (result != null) map.put("result", result);
        if (error != null) map.put("error", error);
        return map;
    }

    public static class CommandRequest {
        public String type;
        public Object payload;
        public Integer timeoutSeconds;
        public String priority;
    }
}
