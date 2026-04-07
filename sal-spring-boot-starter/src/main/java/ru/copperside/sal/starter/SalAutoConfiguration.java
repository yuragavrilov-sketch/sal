package ru.copperside.sal.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main SAL auto-configuration entry point.
 * Registers {@link SalProperties} and imports sub-configurations.
 */
@AutoConfiguration
@EnableConfigurationProperties(SalProperties.class)
public class SalAutoConfiguration {
}
