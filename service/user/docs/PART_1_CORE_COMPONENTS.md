# Part 1: Core Components

## Table of Contents
1. [Filter & Filter Chain](#1-filter--filter-chain)
2. [Authentication Filter (JWT)](#2-authentication-filter-jwt)
3. [Security Context](#3-security-context)
4. [How They Work Together](#4-how-they-work-together)

---

## 1. Filter & Filter Chain

### What is a Filter?

A **filter** is code that intercepts HTTP requests BEFORE they reach your controller. Think of it as a security checkpoint at an airport - every passenger (request) must pass through.

**Real-World Analogy**: 
```
Airport Security Checkpoint
├─ Check boarding pass (authentication)
├─ Security scan (validation)
├─ Customs check (authorization)
└─ Gate entrance (controller/endpoint)
```

### Spring Security Filter Chain

Spring Security uses a **chain** of ~15 filters. Each filter has a specific job:

```
HTTP Request
    ↓
┌─────────────────────────────────────┐
│  SecurityContextPersistenceFilter   │ ← Loads security context
├─────────────────────────────────────┤
│  LogoutFilter                       │ ← Handles logout
├─────────────────────────────────────┤
│  UsernamePasswordAuthenticationFilter │ ← Form login (not used in your app)
├─────────────────────────────────────┤
│  **JwtAuthenticationFilter** ✨     │ ← YOUR CUSTOM FILTER
├─────────────────────────────────────┤
│  ExceptionTranslationFilter         │ ← Converts exceptions to HTTP responses
├─────────────────────────────────────┤
│  FilterSecurityInterceptor          │ ← Checks authorization rules
└─────────────────────────────────────┘
    ↓
Controller (Your endpoint)
```

### Your Configuration

**File**: `SecurityConfig.java` (line 72)
```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**What this does:**
- Adds your custom `JwtAuthenticationFilter` to the chain
- Positions it BEFORE `UsernamePasswordAuthenticationFilter`
- Ensures JWT validation happens early in the chain

### Filter Order Matters!

```java
// ❌ WRONG - Authorization check before authentication
Authorization Filter → Authentication Filter → Controller

// ✅ CORRECT - Authentication first, then authorization
Authentication Filter → Authorization Filter → Controller
```

### Key Characteristics

| Property | Description |
|----------|-------------|
| **Execution** | Every filter executes for every request (unless skipped) |
| **Order** | Defined by Spring Security (you can customize) |
| **Short-circuit** | A filter can stop the chain (e.g., return 401 Unauthorized) |
| **Stateless** | Filters don't store state between requests |

---

## 2. Authentication Filter (JWT)

### What is JwtAuthenticationFilter?

Your custom filter that:
1. Extracts JWT token from `Authorization` header
2. Validates the token
3. Loads user details from database
4. Stores authentication in SecurityContext

**File**: `JwtAuthenticationFilter.java`

### Class Declaration

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    
    // ...
}
```

**Key Points:**
- `@Component` - Spring manages this as a bean
- `OncePerRequestFilter` - Guarantees execution only ONCE per request (important for error handling)
- Dependencies: `JwtService` (token ops) and `UserService` (load user)

### The doFilterInternal Method

This is where the magic happens! Let's break it down:

#### Step 1: Check Public Endpoints (Lines 38-43)

```java
String requestPath = request.getServletPath();
if (requestPath.startsWith("/api/auth/") || requestPath.startsWith("/api/users/public/")) {
    filterChain.doFilter(request, response);  // Skip authentication
    return;
}
```

**Why?**
- Public endpoints (login, register) don't need authentication
- Avoids infinite loops (login would need auth to login!)
- Performance: Skip unnecessary JWT validation

#### Step 2: Extract Authorization Header (Lines 45-50)

```java
final String authHeader = request.getHeader("Authorization");

if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);  // No token, continue chain
    return;
}
```

**Expected header format:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGU...
               ^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
               Prefix JWT Token
```

**What if header is missing?**
- Filter continues to next filter
- Later filters (FilterSecurityInterceptor) will reject unauthorized requests
- Results in **401 Unauthorized** response

#### Step 3: Extract JWT Token (Line 53)

```java
final String jwt = authHeader.substring(7); // Remove "Bearer " prefix
// Result: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGU..."
```

#### Step 4: Extract Username from Token (Line 54)

```java
final String userEmail = jwtService.extractUsername(jwt);
```

**What happens inside JwtService:**
```java
// JwtService.java
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
    // Parses JWT and extracts "sub" claim
}
```

**JWT Structure:**
```json
{
  "sub": "user@example.com",      ← Extracted as username
  "role": ["ROLE_USER"],
  "iat": 1698765432,
  "exp": 1698769032
}
```

#### Step 5: Check Authentication Status (Line 56)

```java
if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    // Proceed with authentication
}
```

**Two conditions:**
1. `userEmail != null` - Token was valid and contained subject
2. `SecurityContextHolder.getContext().getAuthentication() == null` - User not already authenticated

**Why check if already authenticated?**
- Prevents redundant database queries
- Another filter might have already authenticated the user

#### Step 6: Load User Details (Line 58)

```java
UserDetails userDetails = userService.loadUserByUsername(userEmail);
```

**What this does:**
- Calls your `UserService.loadUserByUsername()` method
- Queries database for user with email: `user@example.com`
- Returns `User` entity (implements `UserDetails`)
- Throws `UsernameNotFoundException` if user not found

#### Step 7: Validate Token (Line 60)

```java
if (jwtService.isTokenValid(jwt, userDetails)) {
    // Token is valid, authenticate user
}
```

**Token validation checks:**
```java
// JwtService.java (line 72-75)
public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
}
```

**Validates:**
1. Username in token matches loaded user
2. Token is not expired

#### Step 8: Create Authentication Token (Lines 61-66)

```java
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
    userDetails,                 // principal (the User object)
    null,                        // credentials (cleared for security)
    userDetails.getAuthorities() // authorities (e.g., ["ROLE_USER"])
);
authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
```

**What is this object?**
- Implementation of `Authentication` interface
- Contains user information for current request
- `null` credentials: password not needed after authentication

#### Step 9: Store in SecurityContext (Line 69)

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**Critical moment!** 🎯
- User is now **authenticated** for this request
- Available via `SecurityContextHolder` anywhere in your code
- Authorization filters can now check user's roles/permissions

#### Step 10: Continue Filter Chain (Line 82)

```java
filterChain.doFilter(request, response);
```

**Always called** (unless exception thrown):
- Passes request to next filter
- Eventually reaches your controller
- Response flows back through filters

### Exception Handling (Lines 76-80)

```java
} catch (JwtException e) {
    log.debug("JWT processing error: {}", e.getMessage());
} catch (Exception e) {
    log.error("Error processing JWT: {}", e.getMessage());
}
```

**Why not throw exception?**
- Allows request to continue to authorization filters
- Authorization filters will reject unauthenticated requests
- Results in proper **401 Unauthorized** response

---

## 3. Security Context

### What is SecurityContext?

**SecurityContext** is a thread-local storage that holds authentication information for the **current request**.

**Thread-Local** means:
- Each HTTP request runs on its own thread
- Each thread has its own SecurityContext
- No interference between concurrent requests

### The Three-Layer Structure

```
┌─────────────────────────────────────────┐
│   SecurityContextHolder (Static)        │  ← Static class, entry point
│                                         │
│  ┌───────────────────────────────────┐  │
│  │   SecurityContext (Interface)     │  │  ← Holds authentication
│  │                                   │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │  Authentication (Interface) │  │  │  ← User details + roles
│  │  │                             │  │  │
│  │  │  - Principal: User object   │  │  │
│  │  │  - Credentials: null        │  │  │
│  │  │  - Authorities: [ROLE_USER] │  │  │
│  │  │  - Authenticated: true      │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Setting Authentication

**In JwtAuthenticationFilter (line 69):**
```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**What happens:**
1. Get current thread's SecurityContext
2. Store authentication object in it
3. Available for remainder of request

### Retrieving Current User

**In UserService.getMe() (line 47):**
```java
User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
```

**Breakdown:**
```java
SecurityContextHolder              // Static holder
    .getContext()                  // Get current thread's context
    .getAuthentication()           // Get authentication object
    .getPrincipal()                // Get the User object
```

### Lifecycle

```
Request arrives
    ↓
SecurityContext created (empty)
    ↓
JwtAuthenticationFilter executes
    ↓
Authentication stored in SecurityContext ✅
    ↓
Controller executes (can access user via SecurityContextHolder)
    ↓
Response sent
    ↓
SecurityContext cleared 🗑️
```

### Common Use Cases

#### 1. Get Current User Anywhere
```java
@Service
public class OrderService {
    
    public Order createOrder(OrderRequest request) {
        // Get current authenticated user
        User currentUser = (User) SecurityContextHolder.getContext()
                                      .getAuthentication()
                                      .getPrincipal();
        
        Order order = new Order();
        order.setUser(currentUser);
        // ...
    }
}
```

#### 2. Check User Roles
```java
public boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
}
```

#### 3. Get Username
```java
public String getCurrentUsername() {
    return SecurityContextHolder.getContext()
               .getAuthentication()
               .getName(); // Returns username/email
}
```

### Important Notes ⚠️

**1. SecurityContext is request-scoped**
```java
// ✅ GOOD - Within same request
@GetMapping("/profile")
public UserProfile getProfile() {
    User user = getCurrentUser();  // Works!
    return mapToProfile(user);
}

// ❌ BAD - Async thread
@Async
public void sendEmailAsync() {
    User user = getCurrentUser();  // Returns null! Different thread
}
```

**2. Available only AFTER authentication**
```java
// In public endpoints (before authentication)
SecurityContextHolder.getContext().getAuthentication(); // null or anonymous

// In secured endpoints (after JwtAuthenticationFilter)
SecurityContextHolder.getContext().getAuthentication(); // Your User object
```

**3. Cleared automatically**
- No need to manually clear
- Spring Security does it via `SecurityContextPersistenceFilter`

---

## 4. How They Work Together

### Complete Request Flow

```
┌──────────────────────────────────────────────────────────────────┐
│  Client Request: GET /api/users/me                               │
│  Header: Authorization: Bearer eyJhbGc...                        │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   FILTER CHAIN STARTS          │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  SecurityContextPersistence     │
        │  - Creates empty SecurityContext│
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────────┐
        │  JwtAuthenticationFilter ✨                │
        │                                            │
        │  1. Extract JWT from header                │
        │     jwt = "eyJhbGc..."                     │
        │                                            │
        │  2. Extract username from JWT              │
        │     username = "user@example.com"          │
        │                                            │
        │  3. Load user from database                │
        │     user = userService.loadUserByUsername()│
        │                                            │
        │  4. Validate token                         │
        │     isValid = jwtService.isTokenValid()    │
        │                                            │
        │  5. Create authentication object           │
        │     authToken = new UsernamePassword...    │
        │                                            │
        │  6. Store in SecurityContext ✅            │
        │     SecurityContextHolder.set(authToken)   │
        └────────────┬───────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  FilterSecurityInterceptor     │
        │  - Checks if authenticated ✓   │
        │  - Checks authorization rules ✓│
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────────────┐
        │  UserController.getMe()                    │
        │                                            │
        │  User user = SecurityContextHolder         │
        │      .getContext()                         │
        │      .getAuthentication()                  │
        │      .getPrincipal();                      │
        │                                            │
        │  return UserResponse(user);                │
        └────────────┬───────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  Response sent to client       │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  SecurityContext cleared 🗑️    │
        └────────────────────────────────┘
```

### Interaction Summary

| Component | Role | When | Output |
|-----------|------|------|--------|
| **Filter Chain** | Manages request flow | Every request | Orchestrates all filters |
| **JwtAuthenticationFilter** | Validates JWT | Requests with JWT token | Authentication object |
| **SecurityContext** | Stores current user | During request | Available to all code |
| **JwtService** | Token operations | During validation | Username, validity |
| **UserService** | Loads user from DB | During validation | UserDetails object |

---

## Key Takeaways 🎯

1. **Filters execute in order** - Your JWT filter runs early in the chain
2. **JwtAuthenticationFilter is the bridge** - Connects JWT tokens to Spring Security
3. **SecurityContext is temporary** - Only exists during current request
4. **Authentication happens once per request** - Result stored in SecurityContext
5. **Public endpoints skip authentication** - Checked explicitly in filter

---

## Next Steps

Continue to **[Part 2: Authentication Layer](./PART_2_AUTHENTICATION_LAYER.md)** to learn about:
- AuthenticationManager
- AuthenticationProvider
- PasswordEncoder
- UserDetailsService

---

[← Back to Index](./SPRING_SECURITY_INDEX.md) | [Next: Part 2 →](./PART_2_AUTHENTICATION_LAYER.md)
