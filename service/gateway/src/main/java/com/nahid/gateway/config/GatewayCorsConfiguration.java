package com.nahid.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
public class GatewayCorsConfiguration {

    private final GatewaySecurityProperties securityProperties;

    public GatewayCorsConfiguration(GatewaySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        applyIfNotEmpty(securityProperties.getAllowedOrigins(), corsConfiguration::setAllowedOrigins);
        applyIfNotEmpty(securityProperties.getAllowedOriginPatterns(), corsConfiguration::setAllowedOriginPatterns);
        applyIfNotEmpty(securityProperties.getAllowedMethods(), corsConfiguration::setAllowedMethods);
        applyIfNotEmpty(securityProperties.getAllowedHeaders(), corsConfiguration::setAllowedHeaders);
        applyIfNotEmpty(securityProperties.getExposedHeaders(), corsConfiguration::setExposedHeaders);

        corsConfiguration.setAllowCredentials(securityProperties.isAllowCredentials());

        Duration maxAge = securityProperties.getCorsMaxAge();
        if (maxAge != null) {
            corsConfiguration.setMaxAge(maxAge.getSeconds());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }

    private static void applyIfNotEmpty(List<String> values, java.util.function.Consumer<List<String>> consumer) {
        if (!values.isEmpty()) {
            consumer.accept(values);
        }
    }
}
