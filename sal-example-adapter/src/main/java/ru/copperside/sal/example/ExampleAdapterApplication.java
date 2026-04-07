package ru.copperside.sal.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reference SAL adapter implementation — not for production use.
 * <p>
 * Demonstrates how a client adapter integrates with sal-spring-boot-starter.
 */
@SpringBootApplication
public class ExampleAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleAdapterApplication.class, args);
    }
}
