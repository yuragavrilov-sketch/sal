package ru.copperside.sal.example.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.copperside.sal.api.command.CommandHandler;

/**
 * Example command handler — echoes the payload.
 * Demonstrates how a client adapter registers a command handler.
 */
@Component
public class EchoCommandHandler implements CommandHandler<EchoCommand, EchoResult> {

    private static final Logger log = LoggerFactory.getLogger(EchoCommandHandler.class);

    @Override
    public EchoResult execute(EchoCommand command) {
        log.info("Handling EchoCommand: {}", command.getPayload());
        EchoResult result = new EchoResult();
        result.setEcho(command.getPayload());
        return result;
    }
}
