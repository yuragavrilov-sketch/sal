package ru.copperside.sal.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.copperside.sal.api.command.CommandCompletedEvent;
import ru.copperside.sal.api.command.CommandContext;
import ru.copperside.sal.api.command.CommandPriority;
import ru.copperside.sal.api.exception.InfrastructureExceptionDTO;
import ru.copperside.sal.api.message.RecordedMessage;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wire-compatibility tests: verify Java types serialize to JSON
 * that C# adapters can deserialize (PascalCase, matching field names).
 */
class WireCompatibilityTest {

    private ObjectMapper wireMapper;

    @BeforeEach
    void setUp() {
        wireMapper = new ObjectMapper();
        wireMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        wireMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        wireMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        wireMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        wireMapper.registerModule(new JavaTimeModule());
        wireMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        wireMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    void recordedMessage_shouldMatchCSharpJsonFormat() throws Exception {
        var msg = new RecordedMessage();
        msg.setExchangeName("CommandExchange");
        msg.setRoutingKey("TCB.Payment.TransactionCommand");
        msg.setMessageId(42L);
        msg.setCorrelationId("550e8400-e29b-41d4-a716-446655440000");
        msg.setSourceServiceId("payment-adapter");
        msg.setPriority((byte) 5);
        msg.setTimeStamp(Instant.parse("2024-01-15T14:32:05Z"));
        msg.setAdditionalData(Map.of("Session", "compressed-base64"));

        String json = wireMapper.writeValueAsString(msg);
        JsonNode tree = wireMapper.readTree(json);

        assertThat(tree.get("ExchangeName").asText()).isEqualTo("CommandExchange");
        assertThat(tree.get("RoutingKey").asText()).isEqualTo("TCB.Payment.TransactionCommand");
        assertThat(tree.get("MessageId").asLong()).isEqualTo(42L);
        assertThat(tree.get("CorrelationId").asText()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(tree.get("SourceServiceId").asText()).isEqualTo("payment-adapter");
        assertThat(tree.get("Priority").asInt()).isEqualTo(5);
        assertThat(tree.get("AdditionalData").get("Session").asText()).isEqualTo("compressed-base64");

        // Round-trip
        RecordedMessage deserialized = wireMapper.readValue(json, RecordedMessage.class);
        assertThat(deserialized.getMessageId()).isEqualTo(42L);
        assertThat(deserialized.getExchangeName()).isEqualTo("CommandExchange");
    }

    @Test
    void commandContext_shouldPreserveTyopoFieldNames() throws Exception {
        var ctx = new CommandContext();
        ctx.setCommandType("TCB.Payment.TransactionCommand");
        ctx.setCorrelationId("abc-123");
        ctx.setSourceServiceId("adapter-a");
        ctx.setPriority(CommandPriority.Normal);
        ctx.setExcutionServiceId("adapter-b");  // typo preserved
        ctx.setTimeStamp(Instant.parse("2024-01-15T14:32:05Z"));
        ctx.setExcutionTimeStamp(Instant.parse("2024-01-15T14:32:06Z"));

        String json = wireMapper.writeValueAsString(ctx);
        JsonNode tree = wireMapper.readTree(json);

        // Typo fields must be exactly as C# has them
        assertThat(tree.has("ExcutionServiceId")).isTrue();
        assertThat(tree.has("ExcutionTimeStamp")).isTrue();
        assertThat(tree.has("ExcutionDuration")).isFalse(); // null → omitted
        assertThat(tree.get("Priority").asText()).isEqualTo("Normal");

        // Round-trip
        CommandContext deserialized = wireMapper.readValue(json, CommandContext.class);
        assertThat(deserialized.getExcutionServiceId()).isEqualTo("adapter-b");
        assertThat(deserialized.getPriority()).isEqualTo(CommandPriority.Normal);
    }

    @Test
    void infrastructureExceptionDTO_shouldRoundTrip() throws Exception {
        var inner = new InfrastructureExceptionDTO();
        inner.setCode("FatalException");
        inner.setMessage("inner error");

        var dto = new InfrastructureExceptionDTO();
        dto.setExceptionType("Error");
        dto.setCode("ServiceTimeout");
        dto.setMessage("Request timed out");
        dto.setAdapterName("payment-adapter");
        dto.setSourceType("ActionProvider");
        dto.setSourcePath("/api/payment/create");
        dto.setTimeStamp(Instant.parse("2024-01-15T14:32:05Z"));
        dto.setInnerException(inner);

        String json = wireMapper.writeValueAsString(dto);
        JsonNode tree = wireMapper.readTree(json);

        assertThat(tree.get("ExceptionType").asText()).isEqualTo("Error");
        assertThat(tree.get("Code").asText()).isEqualTo("ServiceTimeout");
        assertThat(tree.get("AdapterName").asText()).isEqualTo("payment-adapter");
        assertThat(tree.get("InnerException").get("Code").asText()).isEqualTo("FatalException");

        InfrastructureExceptionDTO deserialized = wireMapper.readValue(json, InfrastructureExceptionDTO.class);
        assertThat(deserialized.getInnerException().getCode()).isEqualTo("FatalException");
    }

    @Test
    void commandCompletedEvent_shouldRoundTrip() throws Exception {
        var ctx = new CommandContext();
        ctx.setCorrelationId("abc-123");
        ctx.setPriority(CommandPriority.High);

        var event = new CommandCompletedEvent();
        event.setResultType("TCB.Payment.TransactionResult, TCB.Payment");
        event.setContext(ctx);
        event.setAdditionalData(Map.of("Session", "compressed"));

        String json = wireMapper.writeValueAsString(event);
        JsonNode tree = wireMapper.readTree(json);

        assertThat(tree.get("ResultType").asText()).contains("TransactionResult");
        assertThat(tree.get("Context").get("Priority").asText()).isEqualTo("High");

        CommandCompletedEvent deserialized = wireMapper.readValue(json, CommandCompletedEvent.class);
        assertThat(deserialized.getContext().getPriority()).isEqualTo(CommandPriority.High);
    }

    @Test
    void commandPriority_valuesMatchCSharp() {
        assertThat(CommandPriority.Idle.getValue()).isEqualTo(0);
        assertThat(CommandPriority.BelowNormal.getValue()).isEqualTo(4);
        assertThat(CommandPriority.Normal.getValue()).isEqualTo(5);
        assertThat(CommandPriority.AboveNormal.getValue()).isEqualTo(6);
        assertThat(CommandPriority.High.getValue()).isEqualTo(9);
        assertThat(CommandPriority.RealTime.getValue()).isEqualTo(10);
    }
}
