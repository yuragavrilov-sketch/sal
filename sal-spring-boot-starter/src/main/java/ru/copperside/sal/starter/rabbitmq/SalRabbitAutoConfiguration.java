package ru.copperside.sal.starter.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

/**
 * SAL RabbitMQ auto-configuration (ADR-004).
 * <p>
 * Configures RabbitTemplate with SalMessageConverter and publisher confirms.
 * Spring AMQP CachingConnectionFactory handles auto-recovery
 * (replaces C# RabbitMQConnectionManager manual reconnect).
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass(CachingConnectionFactory.class)
@Import(SalTopologyConfigurer.class)
public class SalRabbitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SalMessageConverter salMessageConverter(
            @Qualifier("wireObjectMapper") ObjectMapper wireObjectMapper,
            TypeMappingRegistry typeMappingRegistry) {
        return new SalMessageConverter(wireObjectMapper, typeMappingRegistry);
    }

    /**
     * RabbitTemplate configured with SAL message converter and retry (ADR-003).
     */
    @Bean
    @ConditionalOnMissingBean(name = "salRabbitTemplate")
    public RabbitTemplate salRabbitTemplate(
            CachingConnectionFactory connectionFactory,
            SalMessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setRetryTemplate(salRetryTemplate());
        return template;
    }

    /**
     * RabbitAdmin for topology declaration.
     */
    @Bean
    @ConditionalOnMissingBean(RabbitAdmin.class)
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Retry template for publish failures (ADR-003: replaces LiteDB retry buffer).
     * <p>
     * 5 attempts with exponential backoff: 1s → 2s → 4s → 8s → 10s (max).
     */
    private RetryTemplate salRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        var backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOff);

        var retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
