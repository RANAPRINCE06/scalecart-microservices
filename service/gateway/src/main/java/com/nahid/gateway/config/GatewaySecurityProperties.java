package com.nahid.gateway.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class GatewaySecurityProperties {

    @Value("${app.security.public-endpoints}")
    private String publicEndpoints;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Value("${cors.allowed-methods:}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:}")
    private String exposedHeaders;

    @Getter
    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${cors.max-age:}")
    private String maxAge;

    public List<String> getPublicEndpoints() {
        return toList(publicEndpoints);
    }

    public List<String> getAllowedOrigins() {
        return toList(allowedOrigins);
    }

    public List<String> getAllowedOriginPatterns() {
        return toList(allowedOriginPatterns);
    }

    public List<String> getAllowedMethods() {
        return toList(allowedMethods);
    }

    public List<String> getAllowedHeaders() {
        return toList(allowedHeaders);
    }

    public List<String> getExposedHeaders() {
        return toList(exposedHeaders);
    }

    public Duration getCorsMaxAge() {
        return parseDuration(maxAge);
    }

    private static List<String> toList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static Duration parseDuration(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return DurationStyle.detectAndParse(value.trim());
    }
}
