package ru.copperside.sal.starter.command;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.rabbitmq.SalMessageConverter;
import ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration;
import ru.copperside.sal.starter.session.SessionSerializer;

/**
 * Auto-configuration for RabbitMQ command listeners (Phase 6).
 * <p>
 * Creates {@link CommandConsumer}, {@link CommandResultConsumer},
 * their listener containers, and a {@link CommandListenerRegistrar}
 * that declares queues and starts containers on {@code ApplicationReadyEvent}.
 */
@AutoConfiguration(after = {SalRabbitAutoConfiguration.class, CommandBusAutoConfiguration.class})
@ConditionalOnBean(SalRabbitAutoConfiguration.class)
public class CommandListenerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommandConsumer commandConsumer(SalMessageConverter messageConverter,
                                          CommandHandlerRegistry handlerRegistry,
                                          CommandPublisher commandPublisher,
                                          SessionSerializer sessionSerializer) {
        return new CommandConsumer(messageConverter, handlerRegistry,
                commandPublisher, sessionSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandResultConsumer commandResultConsumer(SalMessageConverter messageConverter,
                                                      DefaultCommandBus commandBus) {
        return new CommandResultConsumer(messageConverter, commandBus);
    }

    @Bean(name = "commandListenerContainer")
    @ConditionalOnMissingBean(name = "commandListenerContainer")
    public SimpleMessageListenerContainer commandListenerContainer(
            ConnectionFactory connectionFactory,
            CommandConsumer commandConsumer,
            SalProperties properties) {
        SimpleMessageListenerContainer container =
                new SimpleMessageListenerContainer(connectionFactory);
        container.setMessageListener(commandConsumer);
        container.setConcurrentConsumers(properties.getCommand().getThreads());
        container.setMaxConcurrentConsumers(properties.getCommand().getThreads() * 2);
        container.setPrefetchCount(properties.getCommand().getThreads());
        container.setAutoStartup(false); // started by CommandListenerRegistrar
        return container;
    }

    @Bean(name = "commandResultListenerContainer")
    @ConditionalOnMissingBean(name = "commandResultListenerContainer")
    public SimpleMessageListenerContainer commandResultListenerContainer(
            ConnectionFactory connectionFactory,
            CommandResultConsumer commandResultConsumer,
            SalProperties properties) {
        SimpleMessageListenerContainer container =
                new SimpleMessageListenerContainer(connectionFactory);
        container.setMessageListener(commandResultConsumer);
        container.setConcurrentConsumers(properties.getCommand().getResultThreads());
        container.setAutoStartup(false); // started by CommandListenerRegistrar
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandListenerRegistrar commandListenerRegistrar(
            CommandHandlerRegistry handlerRegistry,
            SalProperties properties,
            RabbitAdmin rabbitAdmin,
            @Qualifier("commandListenerContainer") SimpleMessageListenerContainer commandContainer,
            @Qualifier("commandResultListenerContainer") SimpleMessageListenerContainer resultContainer) {
        return new CommandListenerRegistrar(handlerRegistry, properties, rabbitAdmin,
                commandContainer, resultContainer);
    }
}
