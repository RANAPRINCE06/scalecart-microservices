ScaleCart --- E-Commerce Microservices Platform

A Java 21 and Spring Boot based e-commerce microservices platform
demonstrating service decomposition, API Gateway routing, service
discovery, centralized configuration, PostgreSQL persistence,
Kafka-based messaging, Redis caching, authentication, and distributed
tracing.

Attribution: This repository is based on and extends the
open-source implementation by Ashifur
Nahid.
Original architecture and implementation are retained where
appropriate. My work focuses on understanding, extending, hardening,
and documenting the system.

Overview

ScaleCart is a distributed e-commerce backend composed of independently
deployable services for users, products, orders, payments, and
notifications.

The platform uses Spring Cloud infrastructure for service discovery,
centralized configuration, and API gateway routing, while Kafka is used
for asynchronous event processing.

Technology Stack

Backend

Java 21

Spring Boot 3

Spring Cloud

Spring Security

Spring Cloud Gateway

Spring Cloud OpenFeign

Spring Data JPA

MapStruct

Distributed Systems

Apache Kafka

Eureka Service Discovery

Spring Cloud Config

Saga-based order orchestration

Asynchronous event processing

Data & Infrastructure

PostgreSQL

Redis

Docker

Docker Compose

Observability & API

Zipkin

Spring Boot Actuator

REST APIs

OpenAPI / Swagger

Maven

Architecture

graph TB
    CLIENT[Client]

    GATEWAY[API Gateway]
    EUREKA[Eureka Discovery]
    CONFIG[Config Server]

    USER[User Service]
    PRODUCT[Product Service]
    ORDER[Order Service]
    PAYMENT[Payment Service]
    NOTIFICATION[Notification Service]
    AUDIT[Audit Service]

    POSTGRES[(PostgreSQL)]
    REDIS[(Redis)]
    KAFKA[Kafka]
    ZIPKIN[Zipkin]

    CLIENT --> GATEWAY

    GATEWAY --> USER
    GATEWAY --> PRODUCT
    GATEWAY --> ORDER
    GATEWAY --> PAYMENT
    GATEWAY --> NOTIFICATION

    ORDER --> PRODUCT
    ORDER --> KAFKA
    PAYMENT --> KAFKA

    KAFKA --> NOTIFICATION
    KAFKA --> AUDIT

    PRODUCT --> REDIS

    USER --> POSTGRES
    PRODUCT --> POSTGRES
    ORDER --> POSTGRES
    PAYMENT --> POSTGRES
    NOTIFICATION --> POSTGRES
    AUDIT --> POSTGRES

    USER --> ZIPKIN
    PRODUCT --> ZIPKIN
    ORDER --> ZIPKIN
    PAYMENT --> ZIPKIN
    NOTIFICATION --> ZIPKIN

Repository Structure

Service           Responsibility

config-server   Centralized application configuration
discovery       Eureka service registry
gateway         External API gateway and request routing
user            Authentication, registration, and user management
product         Product catalog and inventory management
order           Order creation and lifecycle orchestration
payment         Payment processing and payment lifecycle
notification    Notification persistence and delivery
audit           Centralized audit event processing

Current Order Flow

Client
  |
  v
API Gateway
  |
  v
Order Service
  |
  +----> User Service
  |
  +----> Product Service
  |          |
  |          +---- Inventory Reservation
  |
  +----> Persist Order
  |
  +----> Confirm Inventory Reservation
  |
  +----> Kafka
            |
            v
      Notification Service

The order service uses a saga-style orchestration flow for inventory
reservation and order persistence. Failures trigger compensation for
previously completed steps.

Current Payment Flow

Client
  |
  v
Payment Service
  |
  +----> Save PROCESSING payment
  |
  +----> Payment processing
  |
  +---- SUCCESS
  |       |
  |       v
  |     Kafka
  |       |
  |       v
  |   Notification Service
  |
  +---- FAILURE

Payment processing uses an idempotency check based on the
order identifier and publishes PaymentResult events and payment notifications through
Kafka.

Reliability & Event-Driven Processing

The following production-oriented reliability mechanisms are implemented:

- **Kafka Consumer Retry Strategy:** Consumers use `DefaultErrorHandler` configured with bounded retries (`FixedBackOff` of 1 second, 3 retry attempts / 4 total attempts). Failure handling logs detailed warning messages per attempt.
- **Dead Letter Topic (DLT) Handling:** When consumer retries are exhausted, `DeadLetterPublishingRecoverer` automatically routes failed records to `<topic>.DLT` (e.g., `payment-notification.DLT`, `order-notification.DLT`, `payment-result.DLT`, `audit-topic.DLT`) and commits the offset to prevent blocking consumer partitions.
- **PaymentResult Event & State Synchronization:** `PaymentServiceImpl` publishes a `PaymentResultEvent` containing `orderId`, `paymentId`, `status`, `amount`, and `timestamp` for BOTH successful (`COMPLETED`) and failed (`FAILED`) payments. The `Order` service's `PaymentResultConsumer` consumes this event:
  - On `COMPLETED`: Order transitions from `PENDING` -> `CONFIRMED`.
  - On `FAILED`: Order transitions from `PENDING` -> `CANCELLED` and inventory reservation is released.
- **Idempotent Event Consumption:** `OrderServiceImpl.processPaymentResult` is idempotent backed by PostgreSQL persistence. If duplicate `PaymentResult` events arrive, state inspection against stored `order.getStatus()` and persistent `order.paymentId` avoids duplicate transitions, database writes, or event re-publications.
- **Producer Reliability:** All producers (`Order`, `Payment`, `Product`, `User`) enforce `acks=all`, `enable.idempotence=true`, and `retries=3`.

API Idempotency

The platform implements persistence-backed API idempotency for order creation (`POST /api/v1/orders`):

- **Header Support:** Clients supply `Idempotency-Key: <key>` in request headers.
- **Persistence Model:** Idempotency records are stored in PostgreSQL (`order_schema.order_idempotency_records`) with a `UNIQUE` database constraint on `idempotency_key`.
- **Request Fingerprinting:** `CreateOrderRequest` is hashed using SHA-256 (`requestHash`).
- **Duplicate Prevention:**
  - **First Request:** Saves `IN_PROGRESS` record, executes order saga, updates status to `COMPLETED` with `orderId`.
  - **Duplicate Request (Same Key + Same Payload):** Returns previously created `OrderDto` with HTTP 200 OK without re-executing order creation or database writes.
  - **Payload Conflict (Same Key + Different Payload):** Returns HTTP 409 Conflict explaining that the `Idempotency-Key` was already used for a different request.
  - **Concurrent Requests:** Database `UNIQUE` constraint prevents concurrent duplicate executions; parallel duplicate requests receive HTTP 409 Conflict.

Resilience & Circuit Breakers

Inter-service communication via OpenFeign is hardened against cascading failures:

```
Client
  |
  v
Order Service
  |
  +---- Feign (2s connect / 5s read timeout) ----> Product Service / User Service
  |                                                        |
  |                                                     failure
  |                                                        |
  |                                                Resilience4j Circuit Breaker
  |                                                        |
  +--------------------------------------------------------+
                           |
                        Fallback (HTTP 503 Service Unavailable + Log)
```

- **Explicit Feign Timeouts:** Configured across services (`connect-timeout`: 2000ms, `read-timeout`: 5000ms).
- **Resilience4j Circuit Breakers:** Configured for `Order` -> `Product` and `Order` -> `User` dependencies (50% failure rate threshold, 10-call sliding window, 10s wait in open state).
- **Protected Mutations:** Non-idempotent business operations (`createOrder`, `processPayment`, `reserveInventory`) are **not** blindly retried on failure to prevent duplicate business executions.
- **Structured Fallbacks:** Fallbacks log operational metrics (service name, operation, reference ID) and return HTTP 503 `ApiResponse` payloads without converting business failures to false 200 OK responses.
- **State Transition Logging:** `ResilienceConfig` registers event consumers to log CircuitBreaker state changes (`CLOSED` -> `OPEN`, `OPEN` -> `HALF_OPEN`).

Engineering Focus

The project is being evolved from the existing implementation toward a
more production-oriented distributed system.

Planned and implemented improvements are tracked explicitly below so
that the repository does not claim functionality before it exists.

Reliability (Completed)

[x] Kafka retry and Dead Letter Topic handling
[x] Bounded consumer retries
[x] Reliable producer configuration (`acks=all`, `enable.idempotence=true`)
[x] Fixed failure handling for notification consumers (no silent ack, proper exception re-throwing)

Order & Payment Consistency (Completed)

[x] Event-driven payment-to-order status updates (`PaymentResult` event)
[x] Persistence-backed idempotent event consumption
[x] Persistence-backed Idempotency-Key support for `POST /api/v1/orders`

Resilience (Completed)

[x] Feign connect/read timeouts (2s connect / 5s read)
[x] Resilience4j circuit breakers (`Order` -> `Product`, `Order` -> `User`)
[x] Controlled retry policies (protecting non-idempotent mutations from blind retries)
[x] Improved downstream failure fallbacks with operational logging and HTTP 503 status


Correct authenticated-user propagation for audit events

Observability

Correlation/request IDs

Improved structured logging

Prometheus metrics

Kafka trace-context propagation

Testing

Unit tests for critical business services

Kafka consumer tests

Integration tests

Testcontainers for PostgreSQL and Kafka

The checklist above represents the engineering roadmap. Items are
marked complete only after implementation and verification.

Local Development

Prerequisites

Java 21

Maven 3.9+

Docker

Docker Compose

Start Infrastructure

docker compose up -d

Build

mvn clean install

Run a Service

cd service/order
mvn spring-boot:run

Services use the shared configuration and service discovery
infrastructure when the required supporting services are running.

API Documentation

The services expose REST APIs and use OpenAPI/Swagger documentation
where configured.

Typical service areas include:

Authentication and users

Products and catalog

Inventory reservations

Orders

Payments

Notifications

Engineering Goals

The main goal of this project is to demonstrate practical backend
engineering beyond basic CRUD:

Microservice architecture

Service-to-service communication

Event-driven architecture

Distributed transaction patterns

Idempotency

Failure recovery

Resilience patterns

Caching

Observability

Automated testing

Containerized deployment

Author / Maintainer

Prince Rana

B.Tech --- Computer Science & Engineering (Artificial Intelligence)

GitHub: https://github.com/RANAPRINCE06

LinkedIn: https://www.linkedin.com/in/prince-rana-0b622b32a