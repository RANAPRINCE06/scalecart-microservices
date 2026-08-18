# Part 5: Advanced Concepts & Best Practices

## Table of Contents
1. [Session Management](#1-session-management)
2. [CORS Configuration](#2-cors-configuration)
3. [Method Security](#3-method-security)
4. [Common Security Pitfalls](#4-common-security-pitfalls)
5. [Best Practices](#5-best-practices)
6. [Production Considerations](#6-production-considerations)
7. [Testing Security](#7-testing-security)

---

## 1. Session Management

### Stateless vs Stateful

**Your Configuration (Stateless):**
```java
// SecurityConfig.java (Lines 68-70)
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

### What Does STATELESS Mean?

| Aspect | Stateful (Traditional) | Stateless (JWT - Your App) |
|--------|----------------------|---------------------------|
| **Session Storage** | Server stores session (memory/Redis) | No server-side session |
| **Client Stores** | Session ID cookie | JWT token |
| **Server Memory** | High (session per user) | Low (no sessions) |
| **Scalability** | Harder (session replication) | Easier (no shared state) |
| **Logout** | Clear server session | Revoke/blacklist token |

### Why Stateless for JWT?

```
Traditional Session (Stateful):
┌─────────┐     Session ID: abc123     ┌─────────┐
│ Client  │ ────────────────────────► │ Server  │
└─────────┘                            │ Memory: │
                                       │ abc123 →│
                                       │  User 1 │
                                       └─────────┘

JWT (Stateless):
┌─────────┐     JWT: eyJhbGc...       ┌─────────┐
│ Client  │ ────────────────────────► │ Server  │
└─────────┘                            │ Memory: │
            Server verifies signature  │ (empty) │
            and extracts user info     └─────────┘
```

### Benefits

1. **Horizontal Scaling** - Any server can validate any token
2. **No Session Synchronization** - No need for Redis/shared storage
3. **Lower Memory** - No session storage per user
4. **Microservices Friendly** - Token works across all services

### Trade-offs

1. **Token Revocation** - Cannot invalidate tokens immediately (need blacklist)
2. **Token Size** - JWT larger than session ID
3. **Security** - If token stolen, valid until expiration

---

## 2. CORS Configuration

### Your Configuration

**File**: `SecurityConfig.java` (Lines 105-119)
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
    configuration.setAllowCredentials(allowCredentials);
    configuration.setExposedHeaders(List.of("Authorization"));
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
}
```

### What is CORS?

**Cross-Origin Resource Sharing** - Browser security that blocks requests from different origins.

**Example:**
```
Frontend: http://localhost:3000  (Origin A)
Backend:  http://localhost:8080  (Origin B)

Without CORS configuration → ❌ Blocked by browser
With CORS configuration → ✅ Allowed
```

### Configuration Breakdown

**1. Allowed Origins**
```properties
# application.properties
cors.allowed-origins=http://localhost:3000,http://localhost:4200
```
- Origins that can make requests to your API
- ⚠️ Never use `*` in production with credentials

**2. Allowed Methods**
```properties
cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
```
- HTTP methods the frontend can use

**3. Allowed Headers**
```properties
cors.allowed-headers=Authorization,Content-Type,Accept
```
- Headers the frontend can send

**4. Allow Credentials**
```properties
cors.allow-credentials=true
```
- Allows cookies and authorization headers
- ⚠️ Cannot use with `allowedOrigins=*`

**5. Exposed Headers**
```java
configuration.setExposedHeaders(List.of("Authorization"));
```
- Headers the frontend can read from response
- Important for JWT in response headers

### Preflight Requests

**Browser sends OPTIONS request before actual request:**
```
OPTIONS /api/users/me
Origin: http://localhost:3000
Access-Control-Request-Method: GET
Access-Control-Request-Headers: Authorization

Response:
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET
Access-Control-Allow-Headers: Authorization
Access-Control-Allow-Credentials: true
```

---

## 3. Method Security

### Enable Method Security

**File**: `SecurityConfig.java` (Line 34)
```java
@EnableMethodSecurity(prePostEnabled = true)
```

### @PreAuthorize Annotation

**Role-Based Access:**
```java
@Service
public class OrderService {
    
    // Only ADMIN role
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
    }
    
    // Only USER or ADMIN role
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)...;
    }
    
    // Custom expression
    @PreAuthorize("hasRole('USER') and #userId == principal.id")
    public UserProfile updateProfile(Long userId, ProfileRequest request) {
        // User can only update their own profile
    }
}
```

### @PostAuthorize Annotation

**Check after method execution:**
```java
@PostAuthorize("returnObject.userId == principal.id")
public Order getOrderById(Long orderId) {
    Order order = orderRepository.findById(orderId)...;
    // Checks if returned order belongs to current user
    return order;
}
```

### @Secured Annotation

**Simpler role checking:**
```java
@Secured("ROLE_ADMIN")
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}

@Secured({"ROLE_USER", "ROLE_ADMIN"})
public UserResponse getCurrentUser() {
    return ...;
}
```

### SpEL Expressions

**Available Variables:**
- `principal` - Current authenticated user
- `authentication` - Full authentication object
- `hasRole('ROLE')` - Check role
- `hasAuthority('AUTHORITY')` - Check authority
- `isAuthenticated()` - Check if authenticated
- `isAnonymous()` - Check if anonymous

**Examples:**
```java
// Check if user owns the resource
@PreAuthorize("#order.userId == principal.id")
public Order updateOrder(Order order) { ... }

// Complex condition
@PreAuthorize("hasRole('ADMIN') or (#userId == principal.id and hasRole('USER'))")
public void updateUser(Long userId) { ... }

// Check method parameter
@PreAuthorize("#username == principal.username")
public UserProfile getProfile(String username) { ... }
```

---

## 4. Common Security Pitfalls

### Pitfall 1: Exposing Passwords

**❌ WRONG:**
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElse(null);
    // User entity includes password field!
}

// Response exposes hashed password:
{
  "id": 1,
  "email": "user@example.com",
  "password": "$2a$12$R9h/cIPz...",  // ⚠️ EXPOSED!
  ...
}
```

**✅ CORRECT:**
```java
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    User user = userRepository.findById(id)...;
    return userMapper.toUserResponse(user);  // DTO without password
}

// Response is safe:
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
  // No password field
}
```

**Or use @JsonIgnore:**
```java
@Entity
public class User implements UserDetails {
    
    @JsonIgnore  // Prevents serialization
    private String password;
}
```

### Pitfall 2: Not Validating Token Expiration

**❌ WRONG:**
```java
public boolean isTokenValid(String token) {
    return extractUsername(token) != null;  // Only checks if parseable
}
```

**✅ CORRECT:**
```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) 
        && !isTokenExpired(token);  // ✅ Check expiration!
}
```

### Pitfall 3: Hardcoding JWT Secret

**❌ WRONG:**
```java
@Value("${jwt.secret}")
private String secret = "mySecretKey123";  // Default value
```

**✅ CORRECT:**
```properties
# application.properties
jwt.secret=${JWT_SECRET:defaultSecretForDevelopmentOnly}

# Production (environment variable)
export JWT_SECRET="your-256-bit-secret-key-here"
```

### Pitfall 4: Public Endpoints Without Explicit Configuration

**❌ WRONG:**
```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().authenticated()  // All endpoints require auth
)

// But you want /api/auth/login to be public!
// This creates infinite loop
```

**✅ CORRECT:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()  // Public first
    .anyRequest().authenticated()  // Then protect rest
)
```

### Pitfall 5: Not Clearing Credentials

**❌ WRONG:**
```java
return new UsernamePasswordAuthenticationToken(
    user,
    user.getPassword(),  // ⚠️ Password kept in memory
    authorities
);
```

**✅ CORRECT:**
```java
return new UsernamePasswordAuthenticationToken(
    user,
    null,  // ✅ Clear credentials after authentication
    authorities
);
```

### Pitfall 6: JWT Token Too Long Expiration

**❌ WRONG:**
```properties
jwt.access-token-expiration=86400000  # 24 hours
```

**✅ CORRECT:**
```properties
jwt.access-token-expiration=900000     # 15 minutes
jwt.refresh-token-expiration=604800000 # 7 days
```

---

## 5. Best Practices

### 1. Password Security

**Strong Password Policy:**
```java
@Size(min = 8, message = "Password must be at least 8 characters")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must contain uppercase, lowercase, digit, and special character")
private String password;
```

**BCrypt Strength:**
```java
new BCryptPasswordEncoder(12);  // ✅ Good balance
// 10 = fast but less secure
// 12 = recommended (your choice)
// 14+ = very secure but slow
```

### 2. JWT Best Practices

**Token Structure:**
```java
// Access Token: Short-lived, contains user info
{
  "sub": "user@example.com",
  "role": ["ROLE_USER"],
  "iat": 1698765432,
  "exp": 1698766332  // 15 minutes
}

// Refresh Token: Long-lived, minimal info
{
  "sub": "user@example.com",
  "iat": 1698765432,
  "exp": 1699370232  // 7 days
}
```

**Token Storage (Client-Side):**
```javascript
// ❌ WRONG: localStorage (vulnerable to XSS)
localStorage.setItem('token', accessToken);

// ✅ BETTER: httpOnly cookie (set by server)
Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict

// ✅ ACCEPTABLE: sessionStorage (cleared on tab close)
sessionStorage.setItem('token', accessToken);
```

### 3. Error Messages

**❌ WRONG - Information Leakage:**
```java
if (!userRepository.existsByEmail(email)) {
    throw new Exception("Email not found");  // Reveals if email exists
}
if (!passwordEncoder.matches(password, user.getPassword())) {
    throw new Exception("Wrong password");  // Reveals email exists
}
```

**✅ CORRECT - Generic Message:**
```java
// DaoAuthenticationProvider handles this correctly
throw new BadCredentialsException("Invalid credentials");
// User can't tell if email or password was wrong
```

### 4. Rate Limiting

**Prevent Brute Force Attacks:**
```java
// Using Bucket4j or similar
@RestController
public class AuthController {
    
    private final RateLimiter loginLimiter = 
        RateLimiter.of("login", RateLimiterConfig.custom()
            .limitForPeriod(5)        // 5 attempts
            .limitRefreshPeriod(Duration.ofMinutes(15))  // per 15 min
            .build());
    
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        RateLimiter.waitForPermission(loginLimiter);
        return authService.login(request);
    }
}
```

### 5. Logging Security Events

**What to Log:**
```java
// ✅ DO log these
log.info("User {} logged in successfully", email);
log.warn("Failed login attempt for user {}", email);
log.warn("JWT token expired for user {}", username);
log.error("Invalid JWT signature detected");

// ❌ DON'T log these
log.debug("Password: {}", password);  // Never log passwords
log.info("JWT Token: {}", jwtToken);  // Don't log full tokens
```

### 6. Token Refresh Strategy

**Your Implementation (Token Rotation):**
```java
// AuthService.refreshToken() (Lines 81-112)
// ✅ GOOD: Delete old refresh token, issue new one
refreshTokenRepository.delete(refreshToken);
return generateTokenAndResponse(user);  // New tokens
```

**Benefits:**
- One-time use refresh tokens
- Limits damage if token leaked
- Detects token theft (reuse attempt)

---

## 6. Production Considerations

### Environment-Specific Configuration

**application-dev.properties:**
```properties
jwt.secret=dev-secret-key-not-for-production
jwt.access-token-expiration=3600000  # 1 hour (longer for dev)
logging.level.com.nahid.userservice.security=DEBUG
```

**application-prod.properties:**
```properties
jwt.secret=${JWT_SECRET}  # From environment variable
jwt.access-token-expiration=900000  # 15 minutes
logging.level.com.nahid.userservice.security=INFO
```

### Secret Key Management

**❌ WRONG:**
```java
@Value("${jwt.secret:myDefaultSecret}")  // Weak default
```

**✅ CORRECT:**
```bash
# Docker
docker run -e JWT_SECRET="your-secure-key" ...

# Kubernetes Secret
kubectl create secret generic jwt-secret \
  --from-literal=secret='your-secure-key'

# application.yml
jwt:
  secret: ${JWT_SECRET}  # No default, force explicit configuration
```

### HTTPS Only

**SecurityConfig addition:**
```java
http
    .requiresChannel(channel -> 
        channel.anyRequest().requiresSecure()  // Force HTTPS
    )
    // ... rest of config
```

### Token Blacklisting (Optional)

**For critical operations:**
```java
@Service
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token, long expirationMs) {
        String jti = extractJti(token);  // JWT ID claim
        redisTemplate.opsForValue()
            .set("blacklist:" + jti, "true", 
                 expirationMs, TimeUnit.MILLISECONDS);
    }
    
    public boolean isBlacklisted(String token) {
        String jti = extractJti(token);
        return redisTemplate.hasKey("blacklist:" + jti);
    }
}
```

### Health Checks

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
    
    // Protected health check with details
    @GetMapping("/health/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "database", "connected",
            "jwt", "configured"
        ));
    }
}
```

---

## 7. Testing Security

### Test Authentication Filter

```java
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtService jwtService;
    
    @Test
    void shouldAuthenticateWithValidToken() throws Exception {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);
        
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldRejectWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void shouldRejectWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }
}
```

### Test Authorization

```java
@Test
@WithMockUser(roles = "USER")
void userCanAccessOwnProfile() throws Exception {
    mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "USER")
void userCannotAccessAdminEndpoint() throws Exception {
    mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "ADMIN")
void adminCanAccessAdminEndpoint() throws Exception {
    mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isOk());
}
```

### Test Password Encoding

```java
@Test
void passwordShouldBeHashed() {
    String rawPassword = "SecurePass123";
    String encoded = passwordEncoder.encode(rawPassword);
    
    assertNotEquals(rawPassword, encoded);
    assertTrue(passwordEncoder.matches(rawPassword, encoded));
    assertFalse(passwordEncoder.matches("WrongPass", encoded));
}

@Test
void samePasswordShouldProduceDifferentHashes() {
    String password = "SecurePass123";
    String hash1 = passwordEncoder.encode(password);
    String hash2 = passwordEncoder.encode(password);
    
    assertNotEquals(hash1, hash2);  // Different due to salt
    assertTrue(passwordEncoder.matches(password, hash1));
    assertTrue(passwordEncoder.matches(password, hash2));
}
```

---

## Quick Reference

### Security Components Cheat Sheet

```
┌───────────────────────────────────────────────────────────┐
│ REGISTRATION                                              │
│ Components: PasswordEncoder                               │
│ Flow: Hash password → Save to DB                          │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ LOGIN                                                     │
│ Components: AuthenticationManager, AuthenticationProvider,│
│            UserDetailsService, PasswordEncoder, JwtService│
│ Flow: Load user → Verify password → Generate tokens      │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ AUTHENTICATED REQUEST                                     │
│ Components: JwtAuthenticationFilter, UserDetailsService,  │
│            JwtService, SecurityContext                    │
│ Flow: Parse JWT → Validate → Load user → Set context     │
└───────────────────────────────────────────────────────────┘
```

---

## Final Thoughts

You now understand:
1. ✅ All 7 Spring Security components
2. ✅ How they connect and interact
3. ✅ Complete registration and login flows
4. ✅ JWT request handling
5. ✅ Best practices and common pitfalls
6. ✅ Production considerations

### Next Steps

1. **Experiment** - Add logging, trace flows in your debugger
2. **Enhance** - Add roles, permissions, method security
3. **Secure** - Implement rate limiting, token blacklisting
4. **Test** - Write comprehensive security tests
5. **Deploy** - Use environment variables, HTTPS, monitoring

---

[← Back to Part 4](./PART_4_JWT_REQUEST_FLOW.md) | [Back to Index](./SPRING_SECURITY_INDEX.md)
