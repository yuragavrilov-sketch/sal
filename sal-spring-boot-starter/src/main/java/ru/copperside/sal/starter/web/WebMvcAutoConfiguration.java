package ru.copperside.sal.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = WebAutoConfiguration.class)
@ConditionalOnWebApplication
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private final EnvironmentKeyInterceptor environmentKeyInterceptor;

    public WebMvcAutoConfiguration(EnvironmentKeyInterceptor environmentKeyInterceptor) {
        this.environmentKeyInterceptor = environmentKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(environmentKeyInterceptor)
                .excludePathPatterns("/actuator/**", "/ping");
    }
}
