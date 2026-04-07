package ru.copperside.sal.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import ru.copperside.sal.starter.SalProperties;
import ru.copperside.sal.starter.session.SessionSerializer;

/**
 * Creates SAL web layer beans: filters, interceptors, and exception handler.
 * MVC registration ({@link WebMvcAutoConfiguration}) runs after this configuration.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AdapterState adapterState() {
        return new AdapterState();
    }

    @Bean
    @ConditionalOnMissingBean
    public EnvironmentKeyInterceptor environmentKeyInterceptor(SalProperties properties) {
        return new EnvironmentKeyInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OfflineCheckInterceptor offlineCheckInterceptor(AdapterState adapterState, SalProperties properties) {
        return new OfflineCheckInterceptor(adapterState, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SalExceptionHandler salExceptionHandler(SalProperties properties) {
        return new SalExceptionHandler(properties);
    }

    @Bean
    public FilterRegistrationBean<SalContextFilter> salContextFilter(SalProperties properties) {
        FilterRegistrationBean<SalContextFilter> bean = new FilterRegistrationBean<>(new SalContextFilter(properties));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SessionFilter> sessionFilter(SessionSerializer sessionSerializer) {
        FilterRegistrationBean<SessionFilter> bean = new FilterRegistrationBean<>(new SessionFilter(sessionSerializer));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(new RequestLoggingFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}
