# JWT Authentication API

A production-grade Spring Boot 3.5.3 REST API with JWT authentication, built with Java 21 and Spring Security 6.x.

## 📖 Spring Security Deep Dive Documentation

**NEW!** Complete Spring Security architecture documentation with step-by-step flows and visual diagrams:

👉 **[Start Here: Spring Security Deep Dive](./docs/SPRING_SECURITY_INDEX.md)** 👈

Learn how all Spring Security components work together:
- **Part 1:** Filter Chain, JWT Authentication Filter, Security Context
- **Part 2:** Authentication Manager, Password Encoder, UserDetailsService
- **Part 3:** Registration & Login Flows (complete step-by-step)
- **Part 4:** JWT Request Validation & Authorization
- **Part 5:** Best Practices, Security Pitfalls, Production Tips

Perfect for understanding Spring Security from the ground up! 🚀

## Features

- ✅ **JWT Authentication** with access & refresh tokens
- ✅ **Spring Security 6.x** with stateless configuration
- ✅ **Token Rotation** - refresh tokens are rotated on each refresh
- ✅ **BCrypt Password Encoding** (strength 12) + **Custom Password Encoding**
- ✅ **Input Validation** with Bean Validation
- ✅ **Error Responses** with proper HTTP status codes
- ✅ **CORS Support** with configurable origins
- ✅ **Method-level Security** with `@PreAuthorize`
- ✅ **PostgreSQL Database** with JPA/Hibernate
- ✅ **MapStruct** for DTO mapping
- ✅ **OpenAPI/Swagger Documentation**
- ✅ **Scheduled Cleanup** of expired tokens
- ✅ **Production-ready Configuration**

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.3**
- **Spring Security 6.x**
- **Spring Data JPA**
- **PostgreSQL**
- **JWT (jjwt 0.12.6)**
- **Lombok**
- **MapStruct**
- **Maven**

## Quick Start

### Prerequisites

- JDK 21+
- PostgreSQL 12+
- Maven 3.6+

### Database Setup

```sql
CREATE DATABASE postgres;
CREATE USER alibou WITH PASSWORD 'alibou';
GRANT ALL PRIVILEGES ON DATABASE postgres TO alibou;
```

### Environment Variables

```bash
# Database (defaults if not set)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=alibou
DB_PASSWORD=alibou

# JWT (configured in application.properties)
JWT_SECRET=Qw8vZ2pLr9sT1uXy4zB7cV6nP0eR5aS3dF8hJ2kL6mN1qW4tU

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:8080
```

### Run the Application

```bash
./mvnw clean install
./mvnw spring-boot:run
```

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login user | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| POST | `/api/auth/logout` | Logout user | Yes |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users/me` | Get current user profile | Yes |

## Request/Response Examples

### Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "securePassword123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900000
  },
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "securePassword123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900000
  },
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

### Get Current User

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "User profile fetched",
  "data": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "USER",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer YOUR_REFRESH_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900000
  },
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

### Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": {
    "message": "Successfully logged out"
  },
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

## Security Configuration

### JWT Token Settings

- **Access Token:** 15 minutes (900,000 ms)
- **Refresh Token:** 7 days (604,800,000 ms)
- **Clock Skew:** 5 minutes (300,000 ms)
- **Algorithm:** HS256
- **Token Rotation:** Enabled

### Password Security

- **Encoding:** BCrypt with strength 12 + Custom Password Encoding
- **Minimum Length:** 8 characters
- **Maximum Length:** 128 characters

### CORS Configuration

Configure allowed origins in `application.properties`:

```properties
cors.allowed-origins=http://localhost:3000,http://localhost:8080
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

## Error Handling

The API returns standardized error responses with ApiResponse wrapper:

```json
{
  "success": false,
  "message": "Validation Failed",
  "data": null,
  "timestamp": "2024-01-01T10:00:00.000Z",
  "errors": [
    {
      "field": "email",
      "message": "Email should be valid",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

## Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI Docs:** http://localhost:8080/v3/api-docs

## Configuration

Key configuration properties in `application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=alibou
spring.datasource.password=alibou
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
jwt.secret=Qw8vZ2pLr9sT1uXy4zB7cV6nP0eR5aS3dF8hJ2kL6mN1qW4tU
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000
jwt.clock-skew=300000

# Scheduled Cleanup (Daily at midnight)
app.refresh-token.cleanup.cron=0 0 0 * * *

# CORS Configuration
cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:8080}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true

# Swagger Configuration
springdoc.swagger-ui.path=/swagger-ui/index.html
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── nahid/
│   │           └── userservice/
│   │               ├── UserServiceApplication.java
│   │               ├── config/
│   │               │   ├── OpenApiConfig.java
│   │               │   ├── SecurityConfig.java
│   │               │   └── SwaggerUiOpener.java
│   │               ├── controller/
│   │               │   ├── AuthController.java
│   │               │   └── UserController.java
│   │               ├── dto/
│   │               │   ├── ApiResponse.java
│   │               │   ├── AuthRequest.java
│   │               │   ├── AuthResponse.java
│   │               │   ├── LogoutRequest.java
│   │               │   ├── LogoutResponse.java
│   │               │   ├── RefreshTokenRequest.java
│   │               │   ├── RegisterRequest.java
│   │               │   ├── RegisterResponse.java
│   │               │   └── UserResponse.java
│   │               ├── entity/
│   │               │   ├── RefreshToken.java
│   │               │   └── User.java
│   │               ├── enums/
│   │               │   └── Role.java
│   │               ├── exception/
│   │               │   ├── AuthenticationException.java
│   │               │   ├── GlobalExceptionHandler.java
│   │               │   └── ResourceNotFoundException.java
│   │               ├── repository/
│   │               │   ├── RefreshTokenRepository.java
│   │               │   └── UserRepository.java
│   │               ├── security/
│   │               │   ├── AdvancedPasswordHasher.java
│   │               │   ├── CustomPasswordEncoder.java
│   │               │   └── JwtAuthenticationFilter.java
│   │               ├── service/
│   │               │   ├── AuthService.java
│   │               │   ├── JwtService.java
│   │               │   ├── RefreshTokenCleanupService.java
│   │               │   └── UserService.java
│   │               └── util/
│   │                   ├── contant/
│   │                   │   ├── ApiResponseConstant.java
│   │                   │   ├── AppConstant.java
│   │                   │   └── ExceptionMessageConstant.java
│   │                   └── helper/
│   │                       └── ApiResponseUtil.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/
            └── nahid/
                └── userservice/
                    └── UserServiceApplicationTests.java
```

## Production Deployment

### Docker Compose Example

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/postgres
      - SPRING_DATASOURCE_USERNAME=alibou
      - SPRING_DATASOURCE_PASSWORD=alibou
      - JWT_SECRET=your_production_jwt_secret_here
      - CORS_ORIGINS=https://yourdomain.com
    depends_on:
      - db
      
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=postgres
      - POSTGRES_USER=alibou
      - POSTGRES_PASSWORD=alibou
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

### Security Recommendations

1. **Use strong JWT secrets** (256+ bit)
2. **Enable HTTPS** in production
3. **Configure proper CORS** origins
4. **Use connection pooling** for database (HikariCP configured)
5. **Enable rate limiting**
6. **Monitor and log** security events
7. **Regularly rotate** JWT secrets
8. **Use environment variables** for secrets

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
