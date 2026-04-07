package ru.copperside.sal.starter.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.copperside.sal.starter.session.SessionSerializer;

/**
 * Auto-configuration for wire-format serialization (ADR-002).
 * <p>
 * Creates a dedicated {@code wireObjectMapper} configured for C# interop:
 * PascalCase property names, non-null inclusion, string enums, ISO 8601 dates.
 */
@AutoConfiguration
public class SalSerializationAutoConfiguration {

    /**
     * ObjectMapper for RabbitMQ wire-format — PascalCase, compatible with C# Newtonsoft.Json.
     * <p>
     * Inject with {@code @Qualifier("wireObjectMapper")}.
     * Standard Spring MVC ObjectMapper (camelCase) remains untouched.
     */
    @Bean
    @Qualifier("wireObjectMapper")
    @ConditionalOnMissingBean(name = "wireObjectMapper")
    public ObjectMapper wireObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // PascalCase to match C# Newtonsoft.Json default
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);

        // Skip nulls — matches C# NullValueHandling.Ignore
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Enums as strings — matches C# StringEnumConverter
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // ISO 8601 dates — matches C# IsoDateTimeConverter
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Tolerate unknown fields from C# side
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public TypeMappingRegistry typeMappingRegistry() {
        return new TypeMappingRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionSerializer sessionSerializer(@Qualifier("wireObjectMapper") ObjectMapper wireObjectMapper) {
        return new SessionSerializer(wireObjectMapper);
    }
}
