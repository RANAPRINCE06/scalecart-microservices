# Spring Security Quick Reference

## Component Cheat Sheet

### When Each Component Is Used

| Component | Registration | Login | JWT Request |
|-----------|-------------|-------|-------------|
| Filter Chain | ✅ (skips auth) | ✅ (skips auth) | ✅ (full validation) |
| JwtAuthenticationFilter | ⏭️ Skipped | ⏭️ Skipped | ✅ **Main actor** |
| SecurityContext | ❌ | ❌ | ✅ Populated |
| AuthenticationManager | ❌ | ✅ **Main actor** | ❌ |
| AuthenticationProvider | ❌ | ✅ Used | ❌ |
| PasswordEncoder.encode() | ✅ Hash password | ❌ | ❌ |
| PasswordEncoder.matches() | ❌ | ✅ Verify password | ❌ |
| UserDetailsService | ❌ | ✅ Load user | ✅ Load user |
| JwtService | ❌ | ✅ Generate tokens | ✅ Validate tokens |

---

## Flow Quick Reference

### Registration
```
POST /register → AuthService → PasswordEncoder.encode() → Database
Result: User created (NO tokens)
```

### Login
```
POST /login → AuthenticationManager → AuthenticationProvider 
→ UserDetailsService (load) → PasswordEncoder.matches() 
→ JwtService (generate) → Return tokens
```

### Authenticated Request
```
GET /api/users/me → JwtAuthenticationFilter → JwtService.validate() 
→ UserDetailsService (load) → SecurityContext.set() → Controller
```

---

## Code Snippets

### Get Current User
```java
// Method 1: SecurityContextHolder
User user = (User) SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();

// Method 2: Controller parameter
@GetMapping("/profile")
public UserResponse getProfile(@AuthenticationPrincipal User user) {
    return userMapper.toResponse(user);
}
```

### Method Security
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }

@PreAuthorize("hasRole('USER') and #userId == principal.id")
public void updateProfile(Long userId) { ... }
```

### Check Roles
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
boolean isAdmin = auth.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
```

---

## Configuration Quick Reference

### SecurityConfig Bean Summary
```java
@Bean SecurityFilterChain filterChain(HttpSecurity http)
// - CSRF disabled
// - CORS enabled
// - Public endpoints: /api/auth/**
// - Session: STATELESS
// - Custom JWT filter added

@Bean PasswordEncoder passwordEncoder()
// BCryptPasswordEncoder(12)

@Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config)
// Gets default authentication manager

@Bean AuthenticationProvider authenticationProvider()
// DaoAuthenticationProvider + UserDetailsService + PasswordEncoder

@Bean CorsConfigurationSource corsConfigurationSource()
// Allowed origins, methods, headers, credentials
```

### Application Properties (Key Settings)
```properties
# JWT
jwt.secret=<256-bit-secret>
jwt.access-token-expiration=900000     # 15 minutes
jwt.refresh-token-expiration=604800000 # 7 days

# CORS
cors.allowed-origins=http://localhost:3000
cors.allow-credentials=true

# Database
spring.jpa.hibernate.ddl-auto=update

# Session
spring.security.stateless=true
```

---

## Common Patterns

### Secure Public Endpoint
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // Public
            .anyRequest().authenticated()  // Everything else
        );
        return http.build();
    }
}
```

### Custom DTO (Hide Password)
```java
// Entity
@Entity
public class User implements UserDetails {
    @JsonIgnore  // Never expose in JSON
    private String password;
}

// Or use DTO
public record UserResponse(
    Long id,
    String email,
    String firstName
    // No password field
) {}
```

### Test with Mock User
```java
@Test
@WithMockUser(username = "test@example.com", roles = "USER")
void testSecuredEndpoint() {
    mockMvc.perform(get("/api/users/me"))
        .andExpect(status().isOk());
}
```

---

## JWT Structure

### Access Token
```json
{
  "sub": "user@example.com",       // Subject (username)
  "role": ["ROLE_USER"],           // User roles
  "iat": 1698765432,               // Issued at
  "exp": 1698766332                // Expires (15 min)
}
```

### Refresh Token
```json
{
  "sub": "user@example.com",       // Subject only
  "iat": 1698765432,               // Issued at
  "exp": 1699370232                // Expires (7 days)
}
```

---

## Password Hashing

### BCrypt Structure
```
$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqXH8fYr3sK1mN2oP3qR4sT5
│││ │  │                                           │
│││ │  └─ Salt (22 chars)                          └─ Hash (31 chars)
│││ └─ Cost factor (12 = 2^12 = 4096 rounds)
││└─ Minor version
│└─ Major version (2a = BCrypt)
└─ Identifier
```

### Usage
```java
// Registration
String hashed = passwordEncoder.encode("password123");
// "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkq..."

// Login
boolean matches = passwordEncoder.matches("password123", hashed);
// true
```

---

## Error Handling

### Common Exceptions

| Exception | Cause | HTTP Status |
|-----------|-------|-------------|
| `UsernameNotFoundException` | User not found in DB | 401 |
| `BadCredentialsException` | Wrong password | 401 |
| `DisabledException` | Account disabled | 401 |
| `LockedException` | Account locked | 401 |
| `ExpiredJwtException` | JWT token expired | 401 |
| `SignatureException` | Invalid JWT signature | 401 |
| `AccessDeniedException` | Insufficient permissions | 403 |

### Generic Error Response
```java
// Always return generic message for security
throw new BadCredentialsException("Invalid credentials");
// Don't reveal: "Email not found" or "Wrong password"
```

---

## Security Best Practices

### ✅ DO
- Use BCrypt with strength 12+
- Set short access token expiry (15 min)
- Use refresh token rotation
- Clear credentials after authentication
- Use HTTPS in production
- Store JWT secret in environment variables
- Return generic error messages
- Validate all user input
- Use @PreAuthorize for method security
- Log security events (not passwords!)

### ❌ DON'T
- Store passwords in plain text
- Log passwords or tokens
- Hardcode JWT secrets
- Use long-lived access tokens
- Expose password in API responses
- Use `allowedOrigins=*` with credentials
- Trust client-side validation only
- Skip HTTPS in production
- Return detailed error messages
- Store JWT in localStorage (XSS risk)

---

## Debugging Tips

### Enable Security Logging
```properties
logging.level.org.springframework.security=DEBUG
logging.level.com.nahid.userservice.security=DEBUG
```

### Common Issues

**"401 Unauthorized" with valid token**
- Check JWT secret matches between services
- Verify token not expired
- Check Authorization header format: `Bearer <token>`

**"CORS error"**
- Add frontend origin to `cors.allowed-origins`
- Ensure `cors.allow-credentials=true` if sending cookies

**"User not found" after login**
- Check `UserDetailsService.loadUserByUsername()` implementation
- Verify database connection
- Check username field (email vs username)

**Password not matching**
- Ensure same BCrypt strength on encode/match
- Check password field not truncated in DB
- Verify no extra whitespace in password

---

## File Reference

| Topic | File | Key Lines |
|-------|------|-----------|
| Security Configuration | `SecurityConfig.java` | 56-75, 84-101 |
| JWT Filter | `JwtAuthenticationFilter.java` | 32-83 |
| JWT Service | `JwtService.java` | 34-75 |
| User Details Service | `UserService.java` | 37-44 |
| Authentication Service | `AuthService.java` | 44-78, 115-133 |
| User Entity | `User.java` | 74-102 |
| Password Encoder | `SecurityConfig.java` | 84-87 |

---

## Testing Endpoints with curl

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","firstName":"Test","lastName":"User"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}'
```

### Get Current User
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer YOUR_REFRESH_TOKEN"
```

---

## Token Expiry Timeline

```
Login at 14:00:00
│
├─ Access Token issued
│  └─ Valid until 14:15:00 (15 minutes)
│
└─ Refresh Token issued
   └─ Valid until 14:00:00 + 7 days

At 14:15:00 - Access token expires
│
└─ Use refresh token to get new pair
   ├─ New access token (expires 14:30:00)
   └─ New refresh token (expires +7 days from now)
```

---

[Back to Index](./SPRING_SECURITY_INDEX.md)
