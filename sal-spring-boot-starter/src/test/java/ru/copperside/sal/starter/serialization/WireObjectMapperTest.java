package ru.copperside.sal.starter.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies wire-format ObjectMapper (ADR-002):
 * PascalCase, non-null, string enums, ISO dates.
 */
class WireObjectMapperTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SalSerializationAutoConfiguration().wireObjectMapper();
    }

    @Test
    void shouldSerializePropertyNamesInPascalCase() throws Exception {
        var dto = new SampleDto("hello", 42, "world");
        String json = mapper.writeValueAsString(dto);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.has("SomeField")).isTrue();
        assertThat(tree.has("Count")).isTrue();
        assertThat(tree.has("someField")).isFalse();
        assertThat(tree.has("count")).isFalse();
    }

    @Test
    void shouldSkipNullValues() throws Exception {
        var dto = new SampleDto("hello", 42, null);
        String json = mapper.writeValueAsString(dto);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.has("NullableField")).isFalse();
    }

    @Test
    void shouldSerializeEnumsAsStrings() throws Exception {
        var dto = new EnumDto(Priority.High);
        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"High\"");
        assertThat(json).doesNotContain("1");
    }

    @Test
    void shouldDeserializeEnumsFromStrings() throws Exception {
        String json = "{\"Priority\":\"High\"}";
        EnumDto dto = mapper.readValue(json, EnumDto.class);

        assertThat(dto.priority).isEqualTo(Priority.High);
    }

    @Test
    void shouldSerializeDatesAsIso8601() throws Exception {
        var dto = new DateDto(Instant.parse("2024-01-15T14:32:05Z"));
        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("2024-01-15");
        assertThat(json).doesNotMatch(".*\\d{13}.*"); // not epoch millis
    }

    @Test
    void shouldToleratUnknownFieldsOnDeserialization() throws Exception {
        String json = "{\"SomeField\":\"test\",\"Count\":1,\"UnknownField\":\"ignored\"}";
        SampleDto dto = mapper.readValue(json, SampleDto.class);

        assertThat(dto.someField).isEqualTo("test");
        assertThat(dto.count).isEqualTo(1);
    }

    @Test
    void shouldRoundTripRecordedMessageLikeStructure() throws Exception {
        var msg = new RecordedMessageStub();
        msg.exchangeName = "CommandExchange";
        msg.routingKey = "TCB.Payment.TransactionCommand";
        msg.messageId = 42L;
        msg.correlationId = "550e8400-e29b-41d4-a716-446655440000";
        msg.sourceServiceId = "payment-adapter";
        msg.priority = (byte) 5;
        msg.additionalData = Map.of("Session", "compressed-base64-here");

        String json = mapper.writeValueAsString(msg);
        JsonNode tree = mapper.readTree(json);

        // Verify PascalCase keys
        assertThat(tree.has("ExchangeName")).isTrue();
        assertThat(tree.has("RoutingKey")).isTrue();
        assertThat(tree.has("MessageId")).isTrue();
        assertThat(tree.has("CorrelationId")).isTrue();
        assertThat(tree.has("SourceServiceId")).isTrue();
        assertThat(tree.has("Priority")).isTrue();
        assertThat(tree.has("AdditionalData")).isTrue();

        // Round-trip
        RecordedMessageStub deserialized = mapper.readValue(json, RecordedMessageStub.class);
        assertThat(deserialized.exchangeName).isEqualTo("CommandExchange");
        assertThat(deserialized.correlationId).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(deserialized.messageId).isEqualTo(42L);
        assertThat(deserialized.additionalData).containsEntry("Session", "compressed-base64-here");
    }

    // --- Test DTOs ---

    static class SampleDto {
        public String someField;
        public int count;
        public String nullableField;

        SampleDto() {}
        SampleDto(String someField, int count, String nullableField) {
            this.someField = someField;
            this.count = count;
            this.nullableField = nullableField;
        }
    }

    enum Priority { Normal, High, RealTime }

    static class EnumDto {
        public Priority priority;
        EnumDto() {}
        EnumDto(Priority priority) { this.priority = priority; }
    }

    static class DateDto {
        public Instant timeStamp;
        DateDto() {}
        DateDto(Instant timeStamp) { this.timeStamp = timeStamp; }
    }

    /** Simplified RecordedMessage for wire-format round-trip testing. */
    static class RecordedMessageStub {
        public String exchangeName;
        public String routingKey;
        public long messageId;
        public String correlationId;
        public String sourceServiceId;
        public byte priority;
        public Map<String, String> additionalData;
    }
}
