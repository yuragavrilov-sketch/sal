package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.rabbitmq.SalRabbitConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares RabbitMQ queues for command handlers and starts listener containers
 * after the application context is fully ready.
 * <p>
 * Runs on {@link ApplicationReadyEvent} so that {@link CommandHandlerRegistry}
 * has finished its {@code afterSingletonsInstantiated()} scan.
 */
public class CommandListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

    private final CommandHandlerRegistry handlerRegistry;
    private final SalProperties properties;
    private final RabbitAdmin rabbitAdmin;
    private final SimpleMessageListenerContainer commandContainer;
    private final SimpleMessageListenerContainer resultContainer;

    public CommandListenerRegistrar(CommandHandlerRegistry handlerRegistry,
                                    SalProperties properties,
                                    RabbitAdmin rabbitAdmin,
                                    SimpleMessageListenerContainer commandContainer,
                                    SimpleMessageListenerContainer resultContainer) {
        this.handlerRegistry = handlerRegistry;
        this.properties = properties;
        this.rabbitAdmin = rabbitAdmin;
        this.commandContainer = commandContainer;
        this.resultContainer = resultContainer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<String> commandQueues = declareCommandQueues();
        if (!commandQueues.isEmpty()) {
            commandContainer.setQueueNames(commandQueues.toArray(String[]::new));
            commandContainer.start();
            log.info("[BUS] Command listener started on {} queue(s): {}",
                    commandQueues.size(), commandQueues);
        } else {
            log.info("[BUS] No command handlers registered — command listener not started");
        }

        String resultQueue = declareResultQueue();
        resultContainer.setQueueNames(resultQueue);
        resultContainer.start();
        log.info("[BUS] Result listener started on queue '{}'", resultQueue);
    }

    private List<String> declareCommandQueues() {
        List<String> queueNames = new ArrayList<>();

        // Declare exchange first — declareBinding() requires it to already exist
        DirectExchange commandExchange = new DirectExchange(
                SalRabbitConstants.COMMAND_EXCHANGE, true, false);
        rabbitAdmin.declareExchange(commandExchange);

        for (String commandTypeName : handlerRegistry.getRegisteredCommandTypeNames()) {
            String queueName = SalRabbitConstants.COMMAND_QUEUE_PREFIX + commandTypeName;

            Queue queue = QueueBuilder.durable(queueName)
                    .withArgument("x-max-priority", SalRabbitConstants.COMMAND_MAX_PRIORITY)
                    .withArgument("x-dead-letter-exchange", SalRabbitConstants.DEAD_LETTER_EXCHANGE)
                    .build();
            Binding binding = BindingBuilder.bind(queue).to(commandExchange).with(commandTypeName);

            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareBinding(binding);
            queueNames.add(queueName);
            log.info("[BUS] Declared command queue '{}' → routing key '{}'",
                    queueName, commandTypeName);
        }
        return queueNames;
    }

    private String declareResultQueue() {
        String adapterFullName = properties.getAdapter().getType()
                + "." + properties.getAdapter().getName();
        String queueName = adapterFullName + SalRabbitConstants.COMMAND_RESULT_QUEUE_SUFFIX;

        // Declare exchanges first — declareBinding() requires them to already exist
        DirectExchange completedExchange = new DirectExchange(
                SalRabbitConstants.COMMAND_COMPLETED_EXCHANGE, true, false);
        DirectExchange failedExchange = new DirectExchange(
                SalRabbitConstants.COMMAND_FAILED_EXCHANGE, true, false);
        rabbitAdmin.declareExchange(completedExchange);
        rabbitAdmin.declareExchange(failedExchange);

        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", SalRabbitConstants.DEAD_LETTER_EXCHANGE)
                .build();
        rabbitAdmin.declareQueue(queue);

        rabbitAdmin.declareBinding(
                BindingBuilder.bind(queue).to(completedExchange).with(adapterFullName));
        rabbitAdmin.declareBinding(
                BindingBuilder.bind(queue).to(failedExchange).with(adapterFullName));

        log.info("[BUS] Declared result queue '{}' for adapter '{}'", queueName, adapterFullName);
        return queueName;
    }
}
