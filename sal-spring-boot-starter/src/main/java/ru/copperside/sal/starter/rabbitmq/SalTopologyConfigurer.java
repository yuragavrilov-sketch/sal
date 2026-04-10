package ru.copperside.sal.starter.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static ru.copperside.sal.starter.rabbitmq.SalRabbitConstants.*;

/**
 * Declares RabbitMQ infrastructure topology.
 * <p>
 * Creates dead-letter exchange/queue and command/result exchanges.
 * Per-handler command queues are declared dynamically by CommandListenerRegistrar.
 */
@Configuration
@ConditionalOnBean(SalRabbitAutoConfiguration.class)
public class SalTopologyConfigurer {

    @Bean
    public Declarables salInfrastructureTopology() {
        List<Declarable> declarables = new ArrayList<>();

        // Dead letter exchange + queue
        var dlx = new FanoutExchange(DEAD_LETTER_EXCHANGE, true, false);
        var dlq = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        declarables.add(dlx);
        declarables.add(dlq);
        declarables.add(BindingBuilder.bind(dlq).to(dlx));

        // Command exchange (Direct, durable)
        var cmdExchange = new DirectExchange(COMMAND_EXCHANGE, true, false);
        declarables.add(cmdExchange);

        // Command completed/failed exchanges (Direct, durable)
        declarables.add(new DirectExchange(COMMAND_COMPLETED_EXCHANGE, true, false));
        declarables.add(new DirectExchange(COMMAND_FAILED_EXCHANGE, true, false));

        return new Declarables(declarables);
    }
}
