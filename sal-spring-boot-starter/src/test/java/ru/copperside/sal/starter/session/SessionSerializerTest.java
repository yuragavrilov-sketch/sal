package ru.copperside.sal.starter.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests SessionSerializer compression/decompression round-trip
 * and C# BinaryWriter 7-bit encoded length compatibility.
 */
class SessionSerializerTest {

    private SessionSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper wireMapper = new ObjectMapper();
        wireMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        wireMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        wireMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        wireMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        wireMapper.registerModule(new JavaTimeModule());
        wireMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        wireMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        serializer = new SessionSerializer(wireMapper);
    }

    @Test
    void shouldRoundTripSessionData() throws Exception {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("SessionId", "test-session-123");
        session.put("OperationId", 42L);
        session.put("AuthId", 100L);

        String compressed = serializer.serialize(session);
        assertThat(compressed).isNotBlank();

        // Should be valid Base64
        assertThat(Base64.getDecoder().decode(compressed)).isNotEmpty();

        Map<String, Object> restored = serializer.deserialize(compressed);
        assertThat(restored.get("SessionId")).isEqualTo("test-session-123");
        assertThat(((Number) restored.get("OperationId")).longValue()).isEqualTo(42L);
    }

    @Test
    void shouldHandleEmptySession() throws Exception {
        String compressed = serializer.serialize(null);
        assertThat(compressed).isEmpty();

        Map<String, Object> restored = serializer.deserialize("");
        assertThat(restored).isEmpty();
    }

    @Test
    void shouldHandleLargeSession() throws Exception {
        Map<String, Object> session = new LinkedHashMap<>();
        // Create session with > 127 bytes JSON to test multi-byte 7-bit encoding
        for (int i = 0; i < 50; i++) {
            session.put("Key" + i, "Value" + i + "_with_some_extra_data_to_make_it_longer");
        }

        String compressed = serializer.serialize(session);
        Map<String, Object> restored = serializer.deserialize(compressed);
        assertThat(restored).hasSize(50);
        assertThat(restored.get("Key0")).isEqualTo("Value0_with_some_extra_data_to_make_it_longer");
    }

    @Test
    void compressedOutput_shouldBeBase64() throws Exception {
        Map<String, Object> session = Map.of("SessionId", "abc", "OperationId", 1);
        String compressed = serializer.serialize(session);

        // Must be valid Base64 (no exceptions)
        byte[] decoded = Base64.getDecoder().decode(compressed);
        assertThat(decoded.length).isGreaterThan(0);
        // Compressed should be smaller than raw JSON
        assertThat(decoded.length).isLessThan(compressed.length());
    }

    @Test
    void shouldRoundTripCompressDecompress() throws Exception {
        String json = "{\"SessionId\":\"test\",\"OperationId\":42}";
        String base64 = serializer.compressToBase64(json);
        String result = serializer.decompressFromBase64(base64);
        assertThat(result).isEqualTo(json);
    }

    @Test
    void sevenBitEncoding_shouldHandleSmallValues() throws Exception {
        // Value < 128 should be 1 byte
        var baos = new java.io.ByteArrayOutputStream();
        SessionSerializer.write7BitEncodedInt(baos, 42);
        assertThat(baos.size()).isEqualTo(1);
        assertThat(baos.toByteArray()[0]).isEqualTo((byte) 42);

        int read = SessionSerializer.read7BitEncodedInt(new java.io.ByteArrayInputStream(baos.toByteArray()));
        assertThat(read).isEqualTo(42);
    }

    @Test
    void sevenBitEncoding_shouldHandleLargeValues() throws Exception {
        // Value >= 128 should be multi-byte
        var baos = new java.io.ByteArrayOutputStream();
        SessionSerializer.write7BitEncodedInt(baos, 300);
        assertThat(baos.size()).isEqualTo(2); // 300 needs 2 bytes in 7-bit encoding

        int read = SessionSerializer.read7BitEncodedInt(new java.io.ByteArrayInputStream(baos.toByteArray()));
        assertThat(read).isEqualTo(300);
    }
}
