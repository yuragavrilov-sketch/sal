package ru.copperside.sal.starter.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Compresses/decompresses session data for wire-transmission.
 * Must produce output identical to C# SessionSerializer (DeflateStream + BinaryWriter + Base64).
 * <p>
 * C# pipeline: JSON string → BinaryWriter.Write(string) → DeflateStream → Base64
 * <p>
 * BinaryWriter.Write(string) prefixes the UTF-8 bytes with a 7-bit encoded length.
 * We must replicate this encoding exactly.
 */
public class SessionSerializer {

    private final ObjectMapper wireObjectMapper;

    public SessionSerializer(ObjectMapper wireObjectMapper) {
        this.wireObjectMapper = wireObjectMapper;
    }

    /**
     * Serialize session data to compressed Base64 string (C#-compatible).
     */
    public String serialize(Map<String, Object> sessionData) throws IOException {
        if (sessionData == null || sessionData.isEmpty()) {
            return "";
        }
        String json = wireObjectMapper.writeValueAsString(sessionData);
        return compressToBase64(json);
    }

    /**
     * Deserialize compressed Base64 string back to session data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserialize(String base64Data) throws IOException {
        if (base64Data == null || base64Data.isBlank()) {
            return Map.of();
        }
        String json = decompressFromBase64(base64Data);
        return wireObjectMapper.readValue(json, Map.class);
    }

    /**
     * Compress a JSON string using the C# BinaryWriter + DeflateStream + Base64 pipeline.
     */
    public String compressToBase64(String jsonString) throws IOException {
        byte[] utf8Bytes = jsonString.getBytes(StandardCharsets.UTF_8);

        var baos = new ByteArrayOutputStream();
        // C# DeflateStream produces raw DEFLATE (no zlib header/trailer).
        // Java's default Deflater adds a zlib wrapper unless nowrap=true.
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try (var deflaterStream = new DeflaterOutputStream(baos, deflater)) {
            // C# BinaryWriter.Write(string) writes a 7-bit encoded length prefix, then UTF-8 bytes
            write7BitEncodedInt(deflaterStream, utf8Bytes.length);
            deflaterStream.write(utf8Bytes);
        } finally {
            deflater.end();
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Decompress a Base64 string using the C# BinaryReader + DeflateStream pipeline.
     */
    public String decompressFromBase64(String base64Data) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(base64Data);

        // Inflater with nowrap=true to match raw DEFLATE produced by C# DeflateStream.
        Inflater inflater = new Inflater(true);
        try (var inflaterStream = new InflaterInputStream(new ByteArrayInputStream(compressed), inflater)) {
            // C# BinaryReader.ReadString() reads 7-bit encoded length, then UTF-8 bytes
            int length = read7BitEncodedInt(inflaterStream);
            byte[] utf8Bytes = inflaterStream.readNBytes(length);
            return new String(utf8Bytes, StandardCharsets.UTF_8);
        } finally {
            inflater.end();
        }
    }

    /**
     * Write a 7-bit encoded integer (C# BinaryWriter format).
     * Each byte stores 7 bits of the value; the high bit indicates more bytes follow.
     */
    static void write7BitEncodedInt(java.io.OutputStream out, int value) throws IOException {
        int v = value;
        while (v >= 0x80) {
            out.write((v & 0x7F) | 0x80);
            v >>= 7;
        }
        out.write(v);
    }

    /**
     * Read a 7-bit encoded integer (C# BinaryReader format).
     */
    static int read7BitEncodedInt(java.io.InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = in.read();
            if (b < 0) throw new IOException("Unexpected end of stream reading 7-bit encoded int");
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }
}
