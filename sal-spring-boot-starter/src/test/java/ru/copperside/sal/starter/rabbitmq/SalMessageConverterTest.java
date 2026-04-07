package ru.copperside.sal.starter.rabbitmq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests SalMessageConverter AMQP ↔ RecordedMessage mapping.
 */
class SalMessageConverterTest {

    private SalMessageConverter converter;
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

        converter = new SalMessageConverter(wireMapper, new TypeMappingRegistry());
    }

    @Test
    void toMessage_shouldMapAllProperties() throws Exception {
        var rm = new RecordedMessage();
        rm.setPayload(Map.of("Name", "test"));
        rm.setPayloadType("TCB.Payment.TransactionCommand, TCB.Payment");
        rm.setMessageId(42L);
        rm.setCorrelationId("abc-123");
        rm.setSourceServiceId("payment-adapter");
        rm.setPriority((byte) 5);
        rm.setTimeStamp(Instant.parse("2024-01-15T14:32:05Z"));
        rm.setAdditionalData(Map.of("Session", "compressed-data"));

        Message amqpMessage = converter.toMessage(rm, new MessageProperties());
        MessageProperties props = amqpMessage.getMessageProperties();

        assertThat(props.getContentType()).isEqualTo("TCB.Payment.TransactionCommand, TCB.Payment");
        assertThat(props.getMessageId()).isEqualTo("42");
        assertThat(props.getCorrelationId()).isEqualTo("abc-123");
        assertThat(props.getPriority().intValue()).isEqualTo(5);
        assertThat(props.getTimestamp()).isNotNull();
        assertThat((Object) props.getHeader(SalMessageConverter.ADDITIONAL_DATA_HEADER)).isNotNull();
    }

    @Test
    void roundTrip_shouldPreserveRecordedMessage() throws Exception {
        var rm = new RecordedMessage();
        rm.setPayload(Map.of("Amount", 1000, "Currency", "RUB"));
        rm.setPayloadType("TestCommand");
        rm.setMessageId(99L);
        rm.setCorrelationId("corr-456");
        rm.setSourceServiceId("adapter-a");
        rm.setPriority((byte) 9);
        rm.setTimeStamp(Instant.parse("2024-06-01T10:00:00Z"));
        rm.setAdditionalData(Map.of("Session", "test-session"));

        // RecordedMessage → AMQP Message
        MessageProperties outProps = new MessageProperties();
        outProps.setReceivedExchange("TestExchange");
        outProps.setReceivedRoutingKey("test.route");
        Message amqpMessage = converter.toMessage(rm, outProps);

        // Simulate received message with exchange/routing
        MessageProperties inProps = amqpMessage.getMessageProperties();
        inProps.setReceivedExchange("TestExchange");
        inProps.setReceivedRoutingKey("test.route");

        // AMQP Message → RecordedMessage
        RecordedMessage restored = (RecordedMessage) converter.fromMessage(amqpMessage);

        assertThat(restored.getMessageId()).isEqualTo(99L);
        assertThat(restored.getCorrelationId()).isEqualTo("corr-456");
        assertThat(restored.getSourceServiceId()).isEqualTo("adapter-a");
        assertThat(restored.getPriority()).isEqualTo((byte) 9);
        assertThat(restored.getExchangeName()).isEqualTo("TestExchange");
        assertThat(restored.getRoutingKey()).isEqualTo("test.route");
    }

    @Test
    void fromMessage_shouldHandleMissingOptionalFields() {
        MessageProperties props = new MessageProperties();
        props.setContentType("UnknownType");
        props.setReceivedExchange("TestExchange");
        props.setReceivedRoutingKey("test.key");
        byte[] body = "{}".getBytes();

        Message message = new Message(body, props);
        RecordedMessage rm = (RecordedMessage) converter.fromMessage(message);

        assertThat(rm).isNotNull();
        assertThat(rm.getExchangeName()).isEqualTo("TestExchange");
        assertThat(rm.getMessageId()).isEqualTo(0);
    }

    @Test
    void toMessage_shouldIncludeSourceServiceIdInAdditionalData() throws Exception {
        var rm = new RecordedMessage();
        rm.setPayload("test");
        rm.setPayloadType("String");
        rm.setSourceServiceId("my-adapter");
        rm.setTimeStamp(Instant.now());

        Message msg = converter.toMessage(rm, new MessageProperties());
        byte[] headerBytes = msg.getMessageProperties().getHeader(SalMessageConverter.ADDITIONAL_DATA_HEADER);
        assertThat(headerBytes).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, String> additionalData = wireMapper.readValue(headerBytes, Map.class);
        assertThat(additionalData).containsEntry("SourceServiceId", "my-adapter");
    }
}
