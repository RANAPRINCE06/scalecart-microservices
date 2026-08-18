package com.nahid.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                registerStateTransitionLogger(entryAddedEvent.getAddedEntry());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                registerStateTransitionLogger(entryReplacedEvent.getNewEntry());
            }
        };
    }

    private void registerStateTransitionLogger(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.info("CircuitBreaker '{}' state transition: {} -> {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.warn("CircuitBreaker '{}' recorded error [duration={}ms]: {}",
                                event.getCircuitBreakerName(),
                                event.getElapsedDuration().toMillis(),
                                event.getThrowable().getMessage()))
                .onSlowCallRateExceeded(event ->
                        log.warn("CircuitBreaker '{}' slow call rate exceeded: {}%",
                                event.getCircuitBreakerName(), event.getSlowCallRate()));
    }
}
