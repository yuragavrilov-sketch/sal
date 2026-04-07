package ru.copperside.sal.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.copperside.sal.api.command.CommandBus;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandBusAutoConfiguration," +
                "ru.copperside.sal.starter.command.CommandListenerAutoConfiguration"
})
class ExampleAdapterApplicationTest {

    @MockitoBean
    CommandBus commandBus;

    @Test
    void contextLoads() {
    }
}
