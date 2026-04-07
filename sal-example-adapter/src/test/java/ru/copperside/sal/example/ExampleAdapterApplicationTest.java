package ru.copperside.sal.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.api.event.EventBus;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandBusAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandListenerAutoConfiguration," +
                "ru.copperside.sal.starter.event.EventBusAutoConfiguration"
})
class ExampleAdapterApplicationTest {

    // Mocked because RabbitMQ autoconfigurations are excluded
    @MockitoBean
    CommandBus commandBus;

    @MockitoBean
    EventBus eventBus;

    @Test
    void contextLoads() {
        // Smoke test: Spring context starts with sal-spring-boot-starter on classpath
        // RabbitMQ excluded — no broker needed for unit tests
    }
}
