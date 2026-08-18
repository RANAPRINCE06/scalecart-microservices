package com.nahid.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter ordersCreatedCounter(MeterRegistry registry) {
        return Counter.builder("orders_created_total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(registry);
    }

    @Bean
    public Counter kafkaMessagesFailedCounter(MeterRegistry registry) {
        return Counter.builder("kafka_messages_failed_total")
                .description("Total number of Kafka message processing failures")
                .tag("service", "order-service")
                .register(registry);
    }

    @Bean
    public Counter kafkaMessagesDltCounter(MeterRegistry registry) {
        return Counter.builder("kafka_messages_dlt_total")
                .description("Total number of messages sent to Dead Letter Topics")
                .tag("service", "order-service")
                .register(registry);
    }

    @Bean
    public Timer orderCreationTimer(MeterRegistry registry) {
        return Timer.builder("order_creation_duration")
                .description("Duration of order creation business process")
                .tag("service", "order-service")
                .register(registry);
    }
}
