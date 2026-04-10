package ru.copperside.sal.starter.command;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.copperside.sal.api.command.CommandBus;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.lifecycle.AdapterLifecycle;
import ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;
import ru.copperside.sal.starter.session.SessionSerializer;

/**
 * Auto-configuration for Command Bus components.
 */
@AutoConfiguration(after = SalRabbitAutoConfiguration.class)
@ConditionalOnBean(SalRabbitAutoConfiguration.class)
@EnableConfigurationProperties(SalProperties.class)
@EnableScheduling
public class CommandBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommandHandlerRegistry commandHandlerRegistry(
            ApplicationContext applicationContext,
            TypeMappingRegistry typeMappingRegistry) {
        return new CommandHandlerRegistry(applicationContext, typeMappingRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandPublisher commandPublisher(
            RabbitTemplate salRabbitTemplate,
            SessionSerializer sessionSerializer,
            SalProperties properties) {
        return new CommandPublisher(salRabbitTemplate, sessionSerializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean(CommandBus.class)
    public DefaultCommandBus commandBus(
            CommandPublisher commandPublisher,
            TypeMappingRegistry typeMappingRegistry) {
        return new DefaultCommandBus(commandPublisher, typeMappingRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandTimeoutWatcher commandTimeoutWatcher(DefaultCommandBus commandBus) {
        return new CommandTimeoutWatcher(commandBus.pendingCommands);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdapterLifecycle adapterLifecycle(SalProperties properties,
                                             CommandTimeoutWatcher timeoutWatcher) {
        return new AdapterLifecycle(properties, timeoutWatcher);
    }
}
