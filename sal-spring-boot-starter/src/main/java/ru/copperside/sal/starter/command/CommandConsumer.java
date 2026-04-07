package ru.copperside.sal.starter.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import ru.copperside.sal.api.command.Command;
import ru.copperside.sal.api.command.CommandContext;
import ru.copperside.sal.api.command.CommandHandler;
import ru.copperside.sal.api.command.CommandHandlerAsync;
import ru.copperside.sal.api.command.CommandResult;
import ru.copperside.sal.api.command.FailedResult;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.context.CommandContextHolder;
import ru.copperside.sal.starter.context.SalMdc;
import ru.copperside.sal.starter.context.SessionHolder;
import ru.copperside.sal.starter.rabbitmq.SalMessageConverter;
import ru.copperside.sal.starter.rabbitmq.SalRabbitConstants;
import ru.copperside.sal.starter.session.SessionSerializer;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Receives command messages from RabbitMQ, dispatches to registered handlers,
 * and sends results back to the originating adapter.
 * <p>
 * Replaces C# {@code CommandProcessor.ProcessCommand()}.
 * <p>
 * Result routing: completed → {@code COMMAND_COMPLETED_EXCHANGE},
 * failed → {@code COMMAND_FAILED_EXCHANGE}, routing key = {@code SourceServiceId}.
 */
public class CommandConsumer implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger("CommandBus");

    private final SalMessageConverter messageConverter;
    private final CommandHandlerRegistry handlerRegistry;
    private final CommandPublisher commandPublisher;
    private final SessionSerializer sessionSerializer;

    public CommandConsumer(SalMessageConverter messageConverter,
                           CommandHandlerRegistry handlerRegistry,
                           CommandPublisher commandPublisher,
                           SessionSerializer sessionSerializer) {
        this.messageConverter = messageConverter;
        this.handlerRegistry = handlerRegistry;
        this.commandPublisher = commandPublisher;
        this.sessionSerializer = sessionSerializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(Message message) {
        RecordedMessage rm = (RecordedMessage) messageConverter.fromMessage(message);
        String commandTypeName = normalizeTypeName(rm.getPayloadType());

        if (rm.getExpireDate() != null && rm.getExpireDate().isBefore(Instant.now())) {
            log.warn("[BUS] Command {} expired (correlationId={}), skipping",
                    commandTypeName, rm.getCorrelationId());
            return;
        }

        restoreSession(rm);
        SalMdc.set(rm.getCorrelationId(), null, null);
        CommandContextHolder.set(buildCommandContext(rm, commandTypeName));

        try {
            Object handler = handlerRegistry.resolveHandler(commandTypeName);
            if (handler == null) {
                log.error("[BUS] No handler registered for command '{}'", commandTypeName);
                sendFailedResult(rm, "No handler registered for: " + commandTypeName);
                return;
            }

            log.debug("[BUS] Dispatching '{}' correlationId={}", commandTypeName, rm.getCorrelationId());

            Object payload = rm.getPayload();
            CompletableFuture<? extends CommandResult> resultFuture;

            if (handler instanceof CommandHandlerAsync asyncHandler) {
                // Capture context before async execution — ThreadLocals will be
                // cleared in the finally block of the calling thread, so the
                // whenComplete callback running in another thread would see nulls.
                Map<String, Object> capturedSession = SessionHolder.get();
                String capturedCorrelationId = rm.getCorrelationId();

                resultFuture = asyncHandler.executeAsync((Command) payload);

                resultFuture.whenComplete((result, ex) -> {
                    // Restore context in the callback thread
                    if (capturedSession != null) SessionHolder.set(capturedSession);
                    SalMdc.set(capturedCorrelationId, null, null);
                    try {
                        if (ex != null) {
                            log.error("[BUS] '{}' failed correlationId={}",
                                    commandTypeName, capturedCorrelationId, ex);
                            sendFailedResult(rm, ex.getMessage());
                        } else if (result != null) {
                            sendCompletedResult(rm, result);
                        }
                        // null result → fire-and-forget, no reply needed
                    } finally {
                        SessionHolder.clear();
                        SalMdc.clear();
                    }
                });
            } else if (handler instanceof CommandHandler syncHandler) {
                CommandResult result = syncHandler.execute((Command) payload);
                if (result != null) {
                    sendCompletedResult(rm, result);
                }
            } else {
                log.error("[BUS] Unknown handler type {} for '{}'",
                        handler.getClass().getName(), commandTypeName);
                sendFailedResult(rm, "Unknown handler type: " + handler.getClass().getName());
                return;
            }

        } catch (Exception e) {
            log.error("[BUS] Error processing '{}' correlationId={}",
                    commandTypeName, rm.getCorrelationId(), e);
            sendFailedResult(rm, e.getMessage());
        } finally {
            CommandContextHolder.clear();
            SessionHolder.clear();
            SalMdc.clear();
        }
    }

    private void sendCompletedResult(RecordedMessage incomingRm, CommandResult result) {
        if (incomingRm.getSourceServiceId() == null || incomingRm.getSourceServiceId().isBlank()) {
            return; // fire-and-forget: no return address
        }
        RecordedMessage resultRm = buildResultMessage(
                incomingRm, result, SalRabbitConstants.COMMAND_COMPLETED_EXCHANGE);
        commandPublisher.publishResult(resultRm);
        log.debug("[BUS] Result sent correlationId={} → '{}'",
                incomingRm.getCorrelationId(), incomingRm.getSourceServiceId());
    }

    private void sendFailedResult(RecordedMessage incomingRm, String errorMessage) {
        if (incomingRm.getSourceServiceId() == null || incomingRm.getSourceServiceId().isBlank()) {
            return;
        }
        FailedResult failedResult = new FailedResult();
        failedResult.setExeption(errorMessage); // C# typo "Exeption" preserved
        RecordedMessage resultRm = buildResultMessage(
                incomingRm, failedResult, SalRabbitConstants.COMMAND_FAILED_EXCHANGE);
        commandPublisher.publishResult(resultRm);
    }

    private RecordedMessage buildResultMessage(RecordedMessage incomingRm,
                                               CommandResult payload,
                                               String exchange) {
        RecordedMessage resultRm = new RecordedMessage();
        resultRm.setCorrelationId(incomingRm.getCorrelationId());
        resultRm.setPriority(incomingRm.getPriority());
        resultRm.setPayload(payload);
        resultRm.setPayloadType(payload.getClass().getName());
        resultRm.setExchangeName(exchange);
        resultRm.setRoutingKey(incomingRm.getSourceServiceId());
        resultRm.setTimeStamp(Instant.now());
        return resultRm;
    }

    private void restoreSession(RecordedMessage rm) {
        if (rm.getAdditionalData() == null) return;
        String sessionData = rm.getAdditionalData().get("Session");
        if (sessionData != null && !sessionData.isBlank()) {
            try {
                Map<String, Object> session = sessionSerializer.deserialize(sessionData);
                SessionHolder.set(session);
            } catch (IOException e) {
                log.warn("[BUS] Failed to deserialize session", e);
            }
        }
    }

    private CommandContext buildCommandContext(RecordedMessage rm, String commandTypeName) {
        CommandContext ctx = new CommandContext();
        ctx.setCommandType(commandTypeName);
        ctx.setCorrelationId(rm.getCorrelationId());
        ctx.setSourceServiceId(rm.getSourceServiceId());
        ctx.setTimeStamp(rm.getTimeStamp());
        ctx.setExpireDate(rm.getExpireDate());
        return ctx;
    }

    private static String normalizeTypeName(String payloadType) {
        if (payloadType == null) return null;
        int comma = payloadType.indexOf(',');
        return comma >= 0 ? payloadType.substring(0, comma).trim() : payloadType;
    }
}
