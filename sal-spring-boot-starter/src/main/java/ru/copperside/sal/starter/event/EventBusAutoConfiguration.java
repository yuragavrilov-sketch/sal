package ru.copperside.sal.starter.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.copperside.sal.api.event.EventBus;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.rabbitmq.SalRabbitAutoConfiguration;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;
import ru.copperside.sal.starter.session.SessionSerializer;

/**
 * Auto-configuration for Event Bus components (Phase 3).
 */
@AutoConfiguration(after = SalRabbitAutoConfiguration.class)
@ConditionalOnBean(SalRabbitAutoConfiguration.class)
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public DefaultEventBus eventBus(
            RabbitTemplate salRabbitTemplate,
            SessionSerializer sessionSerializer,
            TypeMappingRegistry typeMappingRegistry,
            SalProperties properties) {
        return new DefaultEventBus(salRabbitTemplate, sessionSerializer, typeMappingRegistry, properties);
    }
}
