package ru.copperside.sal.example.command;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.api.command.CommandPriority;

import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates end-to-end command execution via RabbitMQ.
 * <p>
 * Flow: HTTP GET → CommandBus.executeCommandAsync → RabbitMQ → EchoCommandHandler
 *        → result queue → CompletableFuture → HTTP response.
 *
 * <pre>
 *   GET /echo?payload=hello
 * </pre>
 */
@RestController
@RequestMapping("/echo")
public class EchoController {

    private final CommandBus commandBus;

    public EchoController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<String>> echo(
            @RequestParam(defaultValue = "hello") String payload) {

        EchoCommand command = new EchoCommand();
        command.setPayload(payload);

        return commandBus.<EchoResult>executeCommandAsync(command, 30, CommandPriority.Normal)
                .thenApply(result -> ResponseEntity.ok(result.getEcho()))
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body("Command failed: " + ex.getMessage()));
    }
}
