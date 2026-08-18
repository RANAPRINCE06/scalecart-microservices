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

Payment processing currently uses an idempotency check based on the
order identifier and publishes successful payment notifications through
Kafka.

Engineering Focus

The project is being evolved from the existing implementation toward a
more production-oriented distributed system.

Planned and implemented improvements are tracked explicitly below so
that the repository does not claim functionality before it exists.

Reliability

Kafka retry and Dead Letter Topic handling

Bounded consumer retries

Reliable producer configuration

Improved failure handling for notification consumers

Order & Payment Consistency

Event-driven payment-to-order status updates

Idempotency keys for order creation

Improved distributed transaction handling

Payment notification deduplication

Resilience

Feign connect/read timeouts

Resilience4j circuit breakers

Controlled retry policies

Improved downstream failure handling

Security

Consistent downstream authentication

Improved authorization for protected endpoints

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