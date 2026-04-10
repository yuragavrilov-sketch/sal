package ru.copperside.sal.testclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Web UI for probing SAL adapters connected to the same RabbitMQ broker.
 * <p>
 * Connects as an outbound-only SAL participant: it publishes commands by
 * wire-type name and collects results, but does not register any
 * CommandHandler — so no Command_* queue is declared for it.
 */
@SpringBootApplication
public class SalTestClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalTestClientApplication.class, args);
    }
}
