package ru.copperside.sal.starter.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import ru.copperside.sal.api.message.RecordedMessage;
import ru.copperside.sal.starter.serialization.TypeMappingRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts between {@link RecordedMessage} and Spring AMQP {@link Message}.
 * <p>
 * Wire-compatible with C# RabbitMQEventBus property mapping (ADR-002, ADR-004).
 * <p>
 * AMQP property mapping:
 * <ul>
 *   <li>Body → UTF-8 JSON of Payload</li>
 *   <li>ContentType → PayloadType (C# full type name)</li>
 *   <li>MessageId → RecordedMessage.MessageId (as string)</li>
 *   <li>CorrelationId → RecordedMessage.CorrelationId</li>
 *   <li>Priority → RecordedMessage.Priority</li>
 *   <li>DeliveryMode → 2 (persistent)</li>
 *   <li>Timestamp → RecordedMessage.TimeStamp (epoch millis)</li>
 *   <li>Expiration → milliseconds until ExpireDate</li>
 *   <li>Headers["Additional-Data"] → serialized AdditionalData map</li>
 * </ul>
 */
public class SalMessageConverter implements MessageConverter {

    public static final String ADDITIONAL_DATA_HEADER = "Additional-Data";

    private final ObjectMapper wireObjectMapper;
    private final TypeMappingRegistry typeMappingRegistry;

    public SalMessageConverter(ObjectMapper wireObjectMapper, TypeMappingRegistry typeMappingRegistry) {
        this.wireObjectMapper = wireObjectMapper;
        this.typeMappingRegistry = typeMappingRegistry;
    }

    /**
     * Convert a RecordedMessage to an AMQP Message for publishing.
     */
    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (!(object instanceof RecordedMessage rm)) {
            throw new MessageConversionException("Expected RecordedMessage, got " + object.getClass().getName());
        }

        try {
            // Body: serialize Payload to UTF-8 JSON
            byte[] body = wireObjectMapper.writeValueAsBytes(rm.getPayload());

            // AMQP properties
            messageProperties.setContentType(rm.getPayloadType());
            messageProperties.setContentEncoding("UTF-8");
            messageProperties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE); // persistent
            messageProperties.setMessageId(String.valueOf(rm.getMessageId()));
            messageProperties.setCorrelationId(rm.getCorrelationId());
            messageProperties.setPriority(rm.getPriority() & 0xFF);

            if (rm.getTimeStamp() != null) {
                messageProperties.setTimestamp(java.util.Date.from(rm.getTimeStamp()));
            }

            // Expiration: milliseconds from TimeStamp to ExpireDate
            if (rm.getExpireDate() != null && rm.getTimeStamp() != null) {
                long expirationMs = rm.getExpireDate().toEpochMilli() - rm.getTimeStamp().toEpochMilli();
                if (expirationMs > 0) {
                    messageProperties.setExpiration(String.valueOf(expirationMs));
                }
            }

            // Additional-Data header: serialized as JSON bytes
            if (rm.getAdditionalData() != null && !rm.getAdditionalData().isEmpty()) {
                Map<String, String> additionalData = new HashMap<>(rm.getAdditionalData());
                if (rm.getSourceServiceId() != null) {
                    additionalData.put("SourceServiceId", rm.getSourceServiceId());
                }
                byte[] additionalDataBytes = wireObjectMapper.writeValueAsBytes(additionalData);
                messageProperties.setHeader(ADDITIONAL_DATA_HEADER, additionalDataBytes);
            } else if (rm.getSourceServiceId() != null) {
                Map<String, String> additionalData = Map.of("SourceServiceId", rm.getSourceServiceId());
                byte[] additionalDataBytes = wireObjectMapper.writeValueAsBytes(additionalData);
                messageProperties.setHeader(ADDITIONAL_DATA_HEADER, additionalDataBytes);
            }

            return new Message(body, messageProperties);

        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert RecordedMessage to AMQP Message", e);
        }
    }

    /**
     * Convert an AMQP Message back to a RecordedMessage.
     */
    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            MessageProperties props = message.getMessageProperties();
            RecordedMessage rm = new RecordedMessage();

            // Resolve payload type from ContentType header
            String payloadTypeName = props.getContentType();
            rm.setPayloadType(payloadTypeName);

            // Deserialize payload
            if (payloadTypeName != null) {
                // Strip assembly name if present: "Namespace.Class, Assembly" → "Namespace.Class"
                String typeName = payloadTypeName.contains(",")
                        ? payloadTypeName.substring(0, payloadTypeName.indexOf(',')).trim()
                        : payloadTypeName;

                Class<?> payloadClass = typeMappingRegistry.resolveJavaClass(typeName)
                        .or(() -> typeMappingRegistry.resolveJavaClass(payloadTypeName))
                        .or(() -> java.util.Optional.ofNullable(tryLoadClass(typeName)))
                        .orElse(null);

                if (payloadClass != null) {
                    rm.setPayload(wireObjectMapper.readValue(message.getBody(), payloadClass));
                } else {
                    // Fallback: deserialize as generic Map (e.g. unknown C# type)
                    rm.setPayload(wireObjectMapper.readValue(message.getBody(), Map.class));
                }
            } else {
                rm.setPayload(wireObjectMapper.readValue(message.getBody(), Map.class));
            }

            rm.setExchangeName(props.getReceivedExchange());
            rm.setRoutingKey(props.getReceivedRoutingKey());

            // MessageId
            if (props.getMessageId() != null) {
                try {
                    rm.setMessageId(Long.parseLong(props.getMessageId()));
                } catch (NumberFormatException e) {
                    rm.setMessageId(0);
                }
            }

            rm.setCorrelationId(props.getCorrelationId());
            rm.setPriority(props.getPriority() != null ? props.getPriority().byteValue() : 0);

            // Timestamp
            if (props.getTimestamp() != null) {
                rm.setTimeStamp(props.getTimestamp().toInstant());
            }

            // ExpireDate from Expiration
            if (props.getExpiration() != null && rm.getTimeStamp() != null) {
                long expirationMs = Long.parseLong(props.getExpiration());
                rm.setExpireDate(rm.getTimeStamp().plusMillis(expirationMs));
            }

            // Additional-Data header
            Object additionalDataRaw = props.getHeader(ADDITIONAL_DATA_HEADER);
            if (additionalDataRaw instanceof byte[] additionalDataBytes) {
                @SuppressWarnings("unchecked")
                Map<String, String> additionalData = wireObjectMapper.readValue(additionalDataBytes, Map.class);
                rm.setAdditionalData(additionalData);
                String sourceServiceId = additionalData.get("SourceServiceId");
                if (sourceServiceId != null) {
                    rm.setSourceServiceId(sourceServiceId);
                }
            }

            return rm;

        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert AMQP Message to RecordedMessage", e);
        }
    }

    /**
     * Attempt to load a class by name from the current classloader.
     * Used as fallback when the type is not registered in TypeMappingRegistry
     * (e.g. Java-to-Java result messages where the class is known locally).
     */
    private static Class<?> tryLoadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
