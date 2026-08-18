# Gateway Security & Route Management Guide

## Overview
This document explains the JWT-based security implementation in the API Gateway and best practices for managing API endpoints in a microservices architecture.

---

## 🔒 Security Architecture

### 1. JWT Token Flow

```
┌──────────┐      ┌──────────────┐      ┌─────────────────┐      ┌──────────────┐
│  Client  │─────>│   Gateway    │─────>│  User Service   │─────>│  Database    │
│          │      │ (JWT Filter) │      │ (Auth Service)  │      │              │
└──────────┘      └──────────────┘      └─────────────────┘      └──────────────┘
     │                    │                      │
     │ 1. Login Request   │                      │
     │───────────────────>│                      │
     │                    │ 2. Forward to Auth   │
     │                    │─────────────────────>│
     │                    │                      │ 3. Validate & Generate JWT
     │                    │                      │────────────────>
     │                    │ 4. Return JWT Token  │
     │                    │<─────────────────────│
     │ 5. JWT Token       │                      │
     │<───────────────────│                      │
     │                    │                      │
     │ 6. API Request     │                      │
     │ (+ JWT in Header)  │                      │
     │───────────────────>│                      │
     │                    │ 7. Validate JWT      │
     │                    │ (Gateway Filter)     │
     │                    │                      │
     │                    │ 8. Forward Request   │
     │                    │─────────────────────>│
     │                    │    (+ User Info)     │
```

### 2. Security Components

#### A. **JwtUtil** (`util/JwtUtil.java`)
- **Purpose**: Validates JWT tokens at the gateway level
- **Functions**:
  - Extract username from token
  - Extract roles from token
  - Validate token signature and expiration
  - Handle clock skew for distributed systems

#### B. **JwtAuthenticationFilter** (`filter/JwtAuthenticationFilter.java`)
- **Purpose**: Intercepts incoming requests and validates JWT
- **Features**:
  - Checks for public endpoints (no auth required)
  - Validates Authorization header format
  - Verifies JWT token validity
  - Adds user context to downstream requests (X-User-Email, X-User-Roles)
  - Returns structured error responses

#### C. **GatewayConfig** (`config/GatewayConfig.java`)
- **Purpose**: Configures routes with security filters
- **Features**:
  - Applies JWT filter to secured routes
  - Adds retry mechanisms for resilience
  - Load balances across service instances

---

## 🛣️ Route Management Best Practices

### Industry Standards for Managing Many API Endpoints

#### **Option 1: Properties-Based Configuration** ✅ (Simple, Declarative)

**Best for**: Small to medium projects (< 20 routes)

**File**: `gateway-service.properties`

```properties
# Public endpoints (no authentication)
app.security.public-endpoints=/api/auth/login,/api/auth/register,/actuator/health

# User Service Route
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=lb://user-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**
spring.cloud.gateway.routes[0].filters[0]=JwtAuthenticationFilter

# Product Service Route
spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=lb://product-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/v1/products/**
```

**Pros**:
- Easy to understand
- No code changes needed
- Externalized configuration

**Cons**:
- Limited flexibility
- Harder to manage with many routes
- No conditional logic

---

#### **Option 2: Java-Based Configuration** ✅✅ (Flexible, Recommended)

**Best for**: Medium to large projects (20+ routes)

**File**: `GatewayConfig.java`

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("user-service", r -> r
            .path("/api/auth/**")
            .filters(f -> f
                .filter(jwtAuthenticationFilter.apply(new Config()))
                .retry(3))
            .uri("lb://user-service"))
        .build();
}
```

**Pros**:
- Full control over routing logic
- Conditional filters based on method, headers, etc.
- Type-safe configuration
- IDE autocomplete support

**Cons**:
- Requires code deployment for changes
- More verbose than properties

---

#### **Option 3: Hybrid Approach** ✅✅✅ (Best of Both Worlds)

**Best for**: Large projects with diverse requirements

**Strategy**:
1. Use **properties** for static, simple routes
2. Use **Java config** for complex routing logic
3. Use **dynamic discovery** for auto-registration

**Example**:

```java
// Static routes in properties
spring.cloud.gateway.routes[0].id=health-check
spring.cloud.gateway.routes[0].uri=lb://user-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/actuator/health

// Complex routes in Java
@Bean
public RouteLocator complexRoutes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("admin-only", r -> r
            .path("/api/admin/**")
            .filters(f -> f
                .filter(jwtAuthenticationFilter.apply(new Config()))
                .filter(adminAuthorizationFilter.apply(new Config())))
            .uri("lb://user-service"))
        .build();
}
```

---

## 📊 Route Organization Strategies

### 1. **By HTTP Method**
```java
.route("product-read", r -> r
    .path("/api/v1/products/**")
    .and().method(HttpMethod.GET)
    .uri("lb://product-service"))

.route("product-write", r -> r
    .path("/api/v1/products/**")
    .and().method(HttpMethod.POST, HttpMethod.PUT)
    .filters(f -> f.filter(adminFilter))
    .uri("lb://product-service"))
```

### 2. **By Access Level**
```java
.route("public-products", r -> r
    .path("/api/v1/products/public/**")
    .uri("lb://product-service"))

.route("secured-products", r -> r
    .path("/api/v1/products/manage/**")
    .filters(f -> f.filter(jwtFilter))
    .uri("lb://product-service"))
```

### 3. **By Business Domain**
```java
// Order Management Domain
.route("order-creation", r -> r.path("/api/v1/orders/create").uri("lb://order-service"))
.route("order-tracking", r -> r.path("/api/v1/orders/track/**").uri("lb://order-service"))
.route("order-history", r -> r.path("/api/v1/orders/history").uri("lb://order-service"))

// Payment Domain
.route("payment-processing", r -> r.path("/api/v1/payments/**").uri("lb://payment-service"))
```

---

## 🔐 Security Configuration

### 1. **Public Endpoints** (No Authentication)
Define in `gateway-service.properties`:
```properties
app.security.public-endpoints=/api/auth/login,/api/auth/register,/api/auth/refresh,/actuator/health
```

These endpoints bypass JWT validation in `JwtAuthenticationFilter`.

### 2. **Secured Endpoints** (JWT Required)
All endpoints NOT in the public list require a valid JWT token.

**Request Format**:
```http
GET /api/v1/products HTTP/1.1
Host: localhost:8222
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. **Role-Based Access Control (RBAC)**
Roles are extracted from JWT and passed to downstream services:

```http
X-User-Email: user@example.com
X-User-Roles: ROLE_USER,ROLE_ADMIN
```

Downstream services can enforce role-based permissions.

---

## 🚀 Advanced Features

### 1. **Retry Mechanism**
```java
.filters(f -> f.retry(3)) // Retry failed requests 3 times
```

### 2. **Circuit Breaker** (Requires additional dependency)
```java
.filters(f -> f.circuitBreaker(config -> config
    .setName("productService")
    .setFallbackUri("forward:/fallback/products")))
```

### 3. **Rate Limiting** (Requires Redis)
```java
.filters(f -> f.requestRateLimiter(config -> config
    .setRateLimiter(redisRateLimiter())
    .setKeyResolver(userKeyResolver())))
```

### 4. **Request/Response Transformation**
```java
.filters(f -> f
    .addRequestHeader("X-Gateway", "microservices-gateway")
    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis())))
```

---

## 🛡️ Security Best Practices

### ✅ DO:
1. **Always validate JWT at the gateway** - Single point of authentication
2. **Use HTTPS in production** - Encrypt token transmission
3. **Rotate JWT secrets regularly** - Update `jwt.secret` periodically
4. **Set appropriate token expiration** - Balance security vs. UX
5. **Log security events** - Track authentication failures
6. **Add rate limiting** - Prevent brute force attacks
7. **Use strong secrets** - Minimum 256-bit keys for HS256

### ❌ DON'T:
1. **Don't store JWT secrets in source code** - Use environment variables
2. **Don't skip token validation** - Validate on every request
3. **Don't trust client-side validation** - Always validate server-side
4. **Don't expose internal service URLs** - Gateway should be the only entry point
5. **Don't log JWT tokens** - Contains sensitive information

---

## 📝 Configuration Checklist

### For New Microservice
- [ ] Register service in Eureka
- [ ] Add route in `gateway-service.properties` OR `GatewayConfig.java`
- [ ] Determine if endpoints are public or secured
- [ ] Update `app.security.public-endpoints` if needed
- [ ] Test JWT authentication
- [ ] Configure retry/timeout settings
- [ ] Add health check endpoint

### For JWT Configuration
- [ ] Same `jwt.secret` across Gateway and User Service
- [ ] Appropriate `jwt.access-token-expiration` (default: 24h)
- [ ] Appropriate `jwt.refresh-token-expiration` (default: 7 days)
- [ ] Configure `jwt.clock-skew` for distributed systems (default: 5 min)

---

## 🧪 Testing

### Test JWT Authentication
```bash
# 1. Get JWT Token
curl -X POST http://localhost:8222/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Response: {"accessToken": "eyJhbGc...", "refreshToken": "..."}

# 2. Access Secured Endpoint
curl http://localhost:8222/api/v1/products \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Access Without Token (Should fail)
curl http://localhost:8222/api/v1/products
# Response: 401 Unauthorized
```

---

## 📚 Further Reading

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

---

## 🔧 Troubleshooting

### Issue: JWT validation fails
**Solution**: Ensure `jwt.secret` is identical in gateway and user service

### Issue: Routes not working
**Solution**: Check Eureka registration, verify service names match

### Issue: Public endpoints require auth
**Solution**: Update `app.security.public-endpoints` in properties

### Issue: Clock skew errors
**Solution**: Increase `jwt.clock-skew` value (default: 300000ms = 5 minutes)
