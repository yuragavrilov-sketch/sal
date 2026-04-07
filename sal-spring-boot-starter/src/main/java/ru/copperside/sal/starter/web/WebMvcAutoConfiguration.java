package ru.copperside.sal.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers SAL interceptors with Spring MVC.
 * Runs after {@link WebAutoConfiguration} which creates the interceptor beans.
 */
@AutoConfiguration(after = WebAutoConfiguration.class)
@ConditionalOnWebApplication
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private final EnvironmentKeyInterceptor environmentKeyInterceptor;
    private final OfflineCheckInterceptor offlineCheckInterceptor;

    public WebMvcAutoConfiguration(EnvironmentKeyInterceptor environmentKeyInterceptor,
                                   OfflineCheckInterceptor offlineCheckInterceptor) {
        this.environmentKeyInterceptor = environmentKeyInterceptor;
        this.offlineCheckInterceptor = offlineCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(environmentKeyInterceptor)
                .excludePathPatterns("/actuator/**", "/ping");
        registry.addInterceptor(offlineCheckInterceptor)
                .excludePathPatterns("/actuator/**", "/ping");
    }
}
