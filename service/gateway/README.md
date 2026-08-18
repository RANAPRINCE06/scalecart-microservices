# API Gateway Service

## Overview
This is the central API Gateway for the E-commerce microservices architecture. It provides a single entry point for all client requests and handles:
- **JWT Authentication & Authorization**
- **Load Balancing** across service instances
- **Service Discovery** via Eureka
- **Request Routing** to appropriate microservices
- **Resilience** (Retry, Circuit Breaker)
- **Cross-cutting concerns** (Logging, Monitoring)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Port 8222)                │
│  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐   │
│  │ JWT Filter     │  │ Load Balancer│  │ Route Matcher  │   │
│  └────────────────┘  └──────────────┘  └────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐   ┌────────▼────────┐    ┌───────▼────────┐
│ User Service   │   │ Product Service │    │ Order Service  │
│   (Port 8081)  │   │   (Port 8082)   │    │  (Port 8083)   │
└────────────────┘   └─────────────────┘    └────────────────┘
```

## Key Components

### 1. Security Layer
- **`JwtUtil`**: Validates JWT tokens received from clients
- **`JwtAuthenticationFilter`**: Intercepts requests and validates authentication
- **Public Endpoints**: Configured to bypass authentication (login, register, etc.)

### 2. Route Configuration
Two approaches available:
- **Properties-based**: Simple route definitions in `gateway-service.properties`
- **Java-based**: Programmatic routes in `GatewayConfig.java` (recommended for complex logic)

### 3. Service Discovery
Integrates with Eureka Discovery Server to automatically discover and route to service instances.

## Configuration

### Environment Variables
```bash
# JWT Configuration (MUST match User Service)
JWT_SECRET=your-secret-key-here
JWT_ACCESS_TOKEN_EXPIRATION=86400000      # 24 hours
JWT_REFRESH_TOKEN_EXPIRATION=604800000    # 7 days

# Eureka Configuration
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Gateway Port
SERVER_PORT=8222
```

### Public Endpoints (No Authentication Required)
Defined in `gateway-service.properties`:
```properties
app.security.public-endpoints=/api/auth/login,/api/auth/register,/api/auth/refresh,/actuator/health
```

## API Routes

### User Service Routes
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/auth/login` | POST | ❌ | User login |
| `/api/auth/register` | POST | ❌ | User registration |
| `/api/auth/refresh` | POST | ❌ | Refresh token |
| `/api/auth/logout` | POST | ✅ | User logout |
| `/api/auth/me` | GET | ✅ | Get current user |

### Product Service Routes
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/v1/products/**` | GET | ✅ | Get products |
| `/api/v1/products/**` | POST/PUT/DELETE | ✅ | Manage products |
| `/api/v1/categories/**` | ALL | ✅ | Category management |

### Order Service Routes
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/v1/orders/**` | ALL | ✅ | Order management |

### Payment Service Routes
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/v1/payments/**` | ALL | ✅ | Payment processing |

## How JWT Authentication Works

### 1. User Login (Public)
```bash
POST http://localhost:8222/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

# Response
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "role": "ROLE_USER"
}
```

### 2. Accessing Secured Endpoints
```bash
GET http://localhost:8222/api/v1/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Gateway validates JWT and forwards request with user context:
# X-User-Email: user@example.com
# X-User-Roles: ROLE_USER
```

### 3. Authentication Flow
```
1. Client sends request with JWT in Authorization header
2. Gateway extracts token from "Bearer <token>"
3. JwtAuthenticationFilter validates:
   - Token signature
   - Token expiration
   - Token format
4. If valid:
   - Extract user email and roles
   - Add to request headers (X-User-Email, X-User-Roles)
   - Forward to downstream service
5. If invalid:
   - Return 401 Unauthorized
   - No request forwarded
```

## Running the Gateway

### Prerequisites
1. Java 21+
2. Maven
3. Config Server running (port 8888)
4. Eureka Server running (port 8761)

### Start the Gateway
```bash
cd service/gateway
mvn clean install
mvn spring-boot:run
```

### Verify Gateway is Running
```bash
curl http://localhost:8222/actuator/health
```

## Development

### Adding a New Route

#### Option 1: Properties (Simple)
Edit `config-server/src/main/resources/configurations/gateway-service.properties`:
```properties
spring.cloud.gateway.routes[5].id=new-service
spring.cloud.gateway.routes[5].uri=lb://new-service
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/v1/new/**
```

#### Option 2: Java Config (Advanced)
Edit `gateway/src/main/java/com/nahid/gateway/config/GatewayConfig.java`:
```java
.route("new-service", r -> r
    .path("/api/v1/new/**")
    .filters(f -> f
        .filter(jwtAuthenticationFilter.apply(new Config()))
        .retry(3))
    .uri("lb://new-service"))
```

### Adding Public Endpoints
Update in `gateway-service.properties`:
```properties
app.security.public-endpoints=/api/auth/login,/api/auth/register,/api/v1/new-public-endpoint
```

## Troubleshooting

### Issue: 401 Unauthorized for valid token
**Cause**: JWT secret mismatch between Gateway and User Service  
**Solution**: Ensure `jwt.secret` is identical in both services

### Issue: Routes not found (404)
**Cause**: Service not registered in Eureka or wrong service name  
**Solution**: 
1. Check service is running and registered in Eureka (http://localhost:8761)
2. Verify service name matches route URI (e.g., `lb://user-service`)

### Issue: Connection timeout
**Cause**: Downstream service not responding  
**Solution**: 
1. Check service health
2. Adjust timeout in properties:
```properties
spring.cloud.gateway.httpclient.connect-timeout=5000
spring.cloud.gateway.httpclient.response-timeout=10s
```

### Issue: JWT clock skew error
**Cause**: Time difference between gateway and user service  
**Solution**: Increase clock skew tolerance:
```properties
jwt.clock-skew=600000  # 10 minutes
```

## Best Practices

### ✅ DO:
- Use environment variables for sensitive data (JWT secrets)
- Define public endpoints explicitly
- Add retry mechanisms for resilience
- Log important security events
- Use HTTPS in production
- Implement rate limiting
- Monitor gateway metrics

### ❌ DON'T:
- Hardcode JWT secrets
- Skip JWT validation for "internal" endpoints
- Expose internal service URLs to clients
- Log JWT tokens (sensitive data)
- Allow bypass of gateway in production

## Monitoring & Observability

### Actuator Endpoints
```bash
# Health check
GET http://localhost:8222/actuator/health

# Gateway routes
GET http://localhost:8222/actuator/gateway/routes

# Metrics
GET http://localhost:8222/actuator/metrics
```

### Logging
Gateway logs JWT validation events:
```
DEBUG com.nahid.gateway.filter.JwtAuthenticationFilter - Processing request to path: /api/v1/products
DEBUG com.nahid.gateway.filter.JwtAuthenticationFilter - Authenticated user: user@example.com with roles: [ROLE_USER]
```

## Security Considerations

1. **JWT Secret Management**
   - Use strong, random secrets (256+ bits)
   - Rotate secrets periodically
   - Never commit secrets to version control
   - Use environment variables or secret management tools

2. **Token Expiration**
   - Short-lived access tokens (15-60 minutes)
   - Longer-lived refresh tokens (7-30 days)
   - Implement token refresh mechanism

3. **Rate Limiting**
   - Prevent brute force attacks
   - Limit requests per user/IP
   - Use Redis for distributed rate limiting

4. **CORS Configuration**
   - Configure allowed origins
   - Restrict allowed methods and headers
   - Don't use wildcards in production

## Performance Optimization

1. **Connection Pooling**: Gateway reuses connections to services
2. **Load Balancing**: Distributes requests across service instances
3. **Caching**: Consider adding response caching for read-heavy endpoints
4. **Async Processing**: Gateway uses reactive WebFlux for non-blocking I/O

## Documentation

- **[GATEWAY-SECURITY.md](./GATEWAY-SECURITY.md)**: Detailed security architecture and JWT implementation
- **[RouteConfiguration.java](./src/main/java/com/nahid/gateway/config/RouteConfiguration.java)**: Advanced route configuration examples

## Support

For issues or questions, please refer to:
1. Check logs in `logs/gateway.log`
2. Verify Eureka dashboard: http://localhost:8761
3. Review configuration in Config Server
4. Consult GATEWAY-SECURITY.md for security-related issues
