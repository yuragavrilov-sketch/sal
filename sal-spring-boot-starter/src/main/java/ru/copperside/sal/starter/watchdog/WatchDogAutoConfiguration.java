package ru.copperside.sal.starter.watchdog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.client.SalRestClient;
import ru.copperside.sal.starter.health.AdapterStateHealthIndicator;
import ru.copperside.sal.starter.lifecycle.AdapterLifecycle;
import ru.copperside.sal.starter.session.SessionSerializer;
import ru.copperside.sal.starter.web.AdapterState;
import ru.copperside.sal.starter.web.WebAutoConfiguration;

/**
 * Auto-configuration for WatchDog, embedded client, lifecycle, and health monitoring.
 */
@AutoConfiguration(after = WebAutoConfiguration.class)
@EnableScheduling
public class WatchDogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EndPointRegistry endPointRegistry(SalProperties properties) {
        return new EndPointRegistry(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WatchDogService watchDogService(AdapterState adapterState,
                                           EndPointRegistry endPointRegistry,
                                           SalProperties properties,
                                           ApplicationEventPublisher publisher) {
        return new WatchDogService(adapterState, endPointRegistry, properties, publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public SalRestClient salRestClient(SessionSerializer sessionSerializer,
                                       @Qualifier("wireObjectMapper") ObjectMapper wireObjectMapper,
                                       SalProperties properties) {
        RestTemplate restTemplate = new RestTemplate();
        int timeoutMs = properties.getClient().getRequestTimeout() * 1000;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        restTemplate.setRequestFactory(factory);
        return new SalRestClient(restTemplate, sessionSerializer, wireObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EndPointsAvailableScheduler endPointsAvailableScheduler(EndPointRegistry registry,
                                                                    WatchDogService watchDogService,
                                                                    SalRestClient salRestClient,
                                                                    SalProperties properties) {
        return new EndPointsAvailableScheduler(registry, watchDogService, salRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EndPointsRemoverScheduler endPointsRemoverScheduler(EndPointRegistry registry) {
        return new EndPointsRemoverScheduler(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdapterLifecycle adapterLifecycle(AdapterState adapterState, SalProperties properties) {
        return new AdapterLifecycle(adapterState, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdapterStateHealthIndicator adapterStateHealthIndicator(AdapterState adapterState,
                                                                    EndPointRegistry endPointRegistry,
                                                                    SalProperties properties) {
        return new AdapterStateHealthIndicator(adapterState, endPointRegistry, properties);
    }
}
