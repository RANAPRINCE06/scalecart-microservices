package com.nahid.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignCorrelationConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Bean
    public RequestInterceptor feignCorrelationInterceptor() {
        return (RequestTemplate template) -> {
            String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
            if (correlationId != null && !correlationId.isBlank()) {
                template.header(CORRELATION_ID_HEADER, correlationId);
            }
        };
    }
}
