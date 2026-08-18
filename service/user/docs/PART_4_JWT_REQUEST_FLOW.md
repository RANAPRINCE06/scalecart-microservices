# Part 4: JWT Request Flow

## Table of Contents
1. [Authenticated Request Overview](#1-authenticated-request-overview)
2. [Complete Request Flow](#2-complete-request-flow)
3. [JWT Validation Process](#3-jwt-validation-process)
4. [SecurityContext Population](#4-securitycontext-population)
5. [Authorization Check](#5-authorization-check)
6. [Error Scenarios](#6-error-scenarios)

---

## 1. Authenticated Request Overview

### What Happens When User Makes Request?

After login, the client receives JWT tokens. For subsequent requests, the client includes the **access token** in the `Authorization` header.

**Example Request:**
```http
GET /api/users/me HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGU...
```

### Key Difference from Login

| Aspect | Login | Authenticated Request |
|--------|-------|----------------------|
| **Authentication Method** | Username + Password | JWT Token |
| **AuthenticationManager Used** | ✅ Yes | ❌ No |
| **UserDetailsService Called** | ✅ Yes | ✅ Yes (reload user) |
| **PasswordEncoder Used** | ✅ Yes (verify) | ❌ No |
| **Who Authenticates** | DaoAuthenticationProvider | JwtAuthenticationFilter |
| **SecurityContext Set** | ❌ No (public endpoint) | ✅ Yes |

---

## 2. Complete Request Flow

### Step-by-Step Execution

```
┌──────────────────────────────────────────────────────────────┐
│ CLIENT REQUEST                                               │
│ GET /api/users/me HTTP/1.1                                   │
│ Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 1: ENTER FILTER CHAIN                                ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼────────────────────┐
    │ SecurityContextPersistenceFilter        │
    │ - Creates empty SecurityContext         │
    │ - Attaches to current thread            │
    └────────────────────┬────────────────────┘
                         │
                         ▼
    ┌────────────────────────────────────────────────┐
    │ ... (Other Spring Security Filters)            │
    └────────────────────┬───────────────────────────┘
                         │
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 2: JWT AUTHENTICATION FILTER                         ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼────────────────────────────────┐
    │ JwtAuthenticationFilter.doFilterInternal()          │
    │ (Lines 32-83)                                       │
    │                                                     │
    │ Step 1: Check if public endpoint                   │
    │ ────────────────────────────────────────────────── │
    │ String path = request.getServletPath();             │
    │ // "/api/users/me"                                  │
    │                                                     │
    │ if (path.startsWith("/api/auth/")) {                │
    │     return; // Skip JWT validation                  │
    │ }                                                   │
    │                                                     │
    │ Result: NOT public endpoint, continue ▼             │
    └─────────────────────────────────────────────────────┘
                         │
                         ▼
    ┌─────────────────────────────────────────────────────┐
    │ Step 2: Extract Authorization Header               │
    │ ─────────────────────────────────────────────────── │
    │ String authHeader =                                 │
    │     request.getHeader("Authorization");             │
    │                                                     │
    │ Result: "Bearer eyJhbGciOiJIUzI1NiJ9..."           │
    └────────────────────┬────────────────────────────────┘
                         │
                         ▼
    ┌─────────────────────────────────────────────────────┐
    │ Step 3: Validate Header Format                      │
    │ ─────────────────────────────────────────────────── │
    │ if (authHeader == null ||                           │
    │     !authHeader.startsWith("Bearer ")) {            │
    │     filterChain.doFilter(); // Continue             │
    │     return;                                         │
    │ }                                                   │
    │                                                     │
    │ Result: Valid format ✅, continue ▼                 │
    └─────────────────────────────────────────────────────┘
                         │
                         ▼
    ┌─────────────────────────────────────────────────────┐
    │ Step 4: Extract JWT Token                           │
    │ ─────────────────────────────────────────────────── │
    │ String jwt = authHeader.substring(7);                │
    │ // Remove "Bearer " prefix                          │
    │                                                     │
    │ Result: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2hu..."  │
    └────────────────────┬────────────────────────────────┘
                         │
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 3: JWT VALIDATION & USER LOADING                     ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼─────────────────────────────────┐
    │ Step 5: Extract Username from JWT                    │
    │ ───────────────────────────────────────────────────── │
    │ String userEmail = jwtService.extractUsername(jwt);   │
    └────────────────────┬──────────────────────────────────┘
                         │
                         │ Call JwtService
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ JwtService.extractUsername() (Line 63-65)            │
    │                                                      │
    │ return extractClaim(jwt, Claims::getSubject);        │
    │   ↓                                                  │
    │ extractAllClaims(jwt) (Lines 88-114)                 │
    │   ↓                                                  │
    │ Parse JWT:                                           │
    │   1. Split into header.payload.signature            │
    │   2. Decode Base64                                   │
    │   3. Verify signature with secret key                │
    │   4. Check expiration                                │
    │   5. Extract claims                                  │
    │                                                      │
    │ Result: "john@example.com" ✅                        │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ Username extracted
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ Step 6: Check Authentication Status                  │
    │ ────────────────────────────────────────────────────│
    │ if (userEmail != null &&                             │
    │     SecurityContextHolder.getContext()               │
    │         .getAuthentication() == null) {              │
    │     // Not yet authenticated, proceed               │
    │ }                                                    │
    │                                                      │
    │ Result: Need to authenticate ▼                       │
    └──────────────────────────────────────────────────────┘
                         │
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ Step 7: Load User from Database                      │
    │ ────────────────────────────────────────────────────│
    │ UserDetails userDetails =                            │
    │     userService.loadUserByUsername(userEmail);       │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ Query database
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ UserService.loadUserByUsername() (Lines 37-44)       │
    │                                                      │
    │ SELECT * FROM users WHERE email = 'john@example.com' │
    │                                                      │
    │ Returns: User {                                      │
    │   id: 1,                                             │
    │   email: "john@example.com",                         │
    │   password: "$2a$12$R9h...",                         │
    │   role: USER,                                        │
    │   authorities: [ROLE_USER]                           │
    │ }                                                    │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ User loaded ✅
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ Step 8: Validate Token Against User                  │
    │ ────────────────────────────────────────────────────│
    │ if (jwtService.isTokenValid(jwt, userDetails)) {     │
    │     // Token is valid for this user                 │
    │ }                                                    │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ Call JwtService validation
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ JwtService.isTokenValid() (Lines 72-75)              │
    │                                                      │
    │ String username = extractUsername(token);            │
    │ // "john@example.com"                                │
    │                                                      │
    │ Check 1: Username matches?                           │
    │   username.equals(userDetails.getUsername())         │
    │   "john@example.com" == "john@example.com" ✅        │
    │                                                      │
    │ Check 2: Token not expired?                          │
    │   !isTokenExpired(token)                             │
    │   expiryDate > currentDate ✅                        │
    │                                                      │
    │ Result: true ✅                                      │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ Token valid!
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 4: CREATE & STORE AUTHENTICATION                     ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼──────────────────────────────────┐
    │ Step 9: Create Authentication Token                   │
    │ ─────────────────────────────────────────────────────│
    │ UsernamePasswordAuthenticationToken authToken =       │
    │     new UsernamePasswordAuthenticationToken(          │
    │         userDetails,              // Principal        │
    │         null,                     // Credentials      │
    │         userDetails.getAuthorities()  // [ROLE_USER] │
    │     );                                                │
    │                                                       │
    │ authToken.setDetails(                                 │
    │     new WebAuthenticationDetailsSource()              │
    │         .buildDetails(request)                        │
    │ );                                                    │
    │ // Adds IP address, session ID, etc.                 │
    └────────────────────┬──────────────────────────────────┘
                         │
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ Step 10: Store in SecurityContext                    │
    │ ────────────────────────────────────────────────────│
    │ SecurityContextHolder.getContext()                    │
    │     .setAuthentication(authToken);                   │
    │                                                      │
    │ ✅ USER IS NOW AUTHENTICATED FOR THIS REQUEST        │
    └────────────────────┬─────────────────────────────────┘
                         │
                         │ Log authentication
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ log.debug("User {} authenticated successfully",       │
    │     userEmail);                                      │
    └────────────────────┬─────────────────────────────────┘
                         │
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 5: CONTINUE FILTER CHAIN                             ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼─────────────────────────────────┐
    │ filterChain.doFilter(request, response);             │
    │ // Pass to next filter                               │
    └────────────────────┬──────────────────────────────────┘
                         │
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ FilterSecurityInterceptor                            │
    │ - Checks if user is authenticated ✅                 │
    │ - Checks authorization rules                         │
    │ - Allows request to proceed ✅                       │
    └────────────────────┬─────────────────────────────────┘
                         │
                         ▼
╔════════════════════════════════════════════════════════════╗
║ PHASE 6: CONTROLLER EXECUTION                              ║
╚════════════════════════════════════════════════════════════╝
                         │
    ┌────────────────────▼─────────────────────────────────┐
    │ UserController.getMe()                               │
    │                                                      │
    │ @GetMapping("/me")                                   │
    │ public UserResponse getMe() {                        │
    │     return userService.getMe();                      │
    │ }                                                    │
    └────────────────────┬──────────────────────────────────┘
                         │
                         ▼
    ┌──────────────────────────────────────────────────────┐
    │ UserService.getMe() (Lines 46-49)                    │
    │                                                      │
    │ // Get current user from SecurityContext             │
    │ User user = (User) SecurityContextHolder             │
    │     .getContext()                                    │
    │     .getAuthentication()                             │
    │     .getPrincipal();                                 │
    │                                                      │
    │ return userMapper.toUserResponse(user);              │
    └────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│ HTTP RESPONSE                                            │
│ Status: 200 OK                                           │
│                                                          │
│ {                                                        │
│   "id": 1,                                               │
│   "email": "john@example.com",                           │
│   "firstName": "John",                                   │
│   "lastName": "Doe",                                     │
│   "role": "USER"                                         │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
```

---

## 3. JWT Validation Process

### Token Parsing Details

**JWT Structure:**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwicm9sZSI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNjk4NzY1NDMyLCJleHAiOjE2OTg3NjYzMzJ9.Xq5p7K2mN8Rz3Yb4Lc6Td9Wf1Hg8Jk0Mn2Pq5Rs7
└──────────────────┘ └────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘ └──────────────────────────────────────────────┘
    Header                                                      Payload (Claims)                                                                         Signature
```

### Step 1: Header Decoding
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Step 2: Payload Decoding
```json
{
  "sub": "john@example.com",
  "role": ["ROLE_USER"],
  "iat": 1698765432,
  "exp": 1698766332
}
```

### Step 3: Signature Verification

**Process:**
```java
// JwtService.java (Lines 117-120)
private SecretKey getSignInKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
}

// Verification
String computedSignature = HMACSHA256(
    base64UrlEncode(header) + "." + base64UrlEncode(payload),
    secretKey
);

if (computedSignature == providedSignature) {
    // Valid ✅
} else {
    // Invalid - throw SignatureException ❌
}
```

### Step 4: Expiration Check

```java
// JwtService.java (Lines 77-81)
private boolean isTokenExpired(String token) {
    Date expiration = extractExpiration(token);
    // Add clock skew tolerance (5 minutes default)
    return expiration.before(new Date(System.currentTimeMillis() - clockSkew));
}
```

**Example:**
```
Token issued at:  2024-10-20 14:00:00
Token expires at: 2024-10-20 14:15:00 (15 minutes later)
Current time:     2024-10-20 14:10:00

Result: NOT expired ✅ (still 5 minutes left)
```

---

## 4. SecurityContext Population

### What Gets Stored?

**Authentication Object Contents:**
```java
UsernamePasswordAuthenticationToken {
    principal: User {
        id: 1,
        email: "john@example.com",
        firstName: "John",
        lastName: "Doe",
        role: USER,
        password: "$2a$12$R9h...",  // Still there, but not used
        authorities: [SimpleGrantedAuthority("ROLE_USER")]
    },
    credentials: null,  // Cleared for security
    authorities: [SimpleGrantedAuthority("ROLE_USER")],
    authenticated: true,
    details: WebAuthenticationDetails {
        remoteAddress: "192.168.1.100",
        sessionId: null  // Stateless
    }
}
```

### Accessing Current User

**Method 1: Via SecurityContextHolder**
```java
User currentUser = (User) SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();
```

**Method 2: Via Controller Parameter (Spring MVC)**
```java
@GetMapping("/profile")
public UserResponse getProfile(@AuthenticationPrincipal User currentUser) {
    // Spring automatically injects current user
    return userMapper.toResponse(currentUser);
}
```

**Method 3: Via Authentication Parameter**
```java
@GetMapping("/info")
public UserInfo getInfo(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return new UserInfo(user);
}
```

---

## 5. Authorization Check

### SecurityConfig Rules

**File**: `SecurityConfig.java` (Lines 61-67)
```java
.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/api/users/**").permitAll()  // ⚠️ Currently all public!
    
    // Protected endpoints
    .anyRequest().authenticated()
)
```

### How FilterSecurityInterceptor Works

```
Request: GET /api/users/me
    │
    ▼
┌─────────────────────────────────────────────┐
│ FilterSecurityInterceptor                   │
│                                             │
│ 1. Get SecurityContext                      │
│    authentication = SecurityContextHolder   │
│        .getContext().getAuthentication()    │
│                                             │
│ 2. Check if authenticated                   │
│    if (authentication == null ||            │
│        !authentication.isAuthenticated()) { │
│        throw AccessDeniedException();       │
│    }                                        │
│                                             │
│ 3. Check authorization rules                │
│    - Match request path: /api/users/me      │
│    - Required: authenticated()              │
│    - User has: authenticated = true ✅      │
│                                             │
│ 4. Allow request ✅                         │
└─────────────────────────────────────────────┘
```

### Method-Level Security

**Example with @PreAuthorize:**
```java
@Service
public class AdminService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        // Only ADMIN role can execute this
        userRepository.deleteById(userId);
    }
    
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public UserResponse getCurrentUser() {
        // Both USER and ADMIN can execute
        return ...;
    }
}
```

---

## 6. Error Scenarios

### Scenario 1: Missing Authorization Header

**Request:**
```http
GET /api/users/me
(No Authorization header)
```

**Flow:**
```
JwtAuthenticationFilter (Line 47-50)
    ↓
if (authHeader == null) {
    filterChain.doFilter();  // Continue without authentication
    return;
}
    ↓
FilterSecurityInterceptor
    ↓
Authentication is null ❌
    ↓
Response: 401 Unauthorized
```

### Scenario 2: Invalid JWT Token

**Request:**
```http
GET /api/users/me
Authorization: Bearer invalid.jwt.token
```

**Flow:**
```
JwtAuthenticationFilter → JwtService.extractUsername()
    ↓
try {
    Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
} catch (SignatureException e) {
    log.debug("JWT signature validation failed");
}
    ↓
Exception caught (Line 76-80)
    ↓
filterChain.doFilter()  // Continue without authentication
    ↓
FilterSecurityInterceptor → 401 Unauthorized
```

### Scenario 3: Expired Token

**Request:**
```http
GET /api/users/me
Authorization: Bearer <expired_token>
```

**Flow:**
```
JwtService.isTokenValid() (Line 72-75)
    ↓
isTokenExpired(token)
    ↓
expiration.before(currentDate) → true ❌
    ↓
isTokenValid() returns false
    ↓
Authentication not set
    ↓
FilterSecurityInterceptor → 401 Unauthorized
```

**Expected Response:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### Scenario 4: User Deleted After Token Issued

**Request:**
```http
GET /api/users/me
Authorization: Bearer <valid_token_for_deleted_user>
```

**Flow:**
```
JwtAuthenticationFilter → userService.loadUserByUsername()
    ↓
userRepository.findByEmail("deleted@example.com")
    ↓
Optional.empty()
    ↓
throw UsernameNotFoundException
    ↓
Caught in filter (Line 78-80)
    ↓
filterChain.doFilter()  // Continue without authentication
    ↓
401 Unauthorized
```

---

## Key Takeaways 🎯

1. **JwtAuthenticationFilter handles ALL JWT validation** - not AuthenticationManager
2. **SecurityContext is populated for every authenticated request** - thread-local storage
3. **User is reloaded from database** - ensures up-to-date user information
4. **Token validation is multi-step** - signature, expiration, user exists
5. **Errors are handled gracefully** - continue filter chain, let authorization filters reject

---

## Next Steps

Continue to **[Part 5: Advanced Concepts](./PART_5_ADVANCED_CONCEPTS.md)** for best practices, common pitfalls, and advanced security topics.

---

[← Back to Part 3](./PART_3_REGISTRATION_LOGIN_FLOWS.md) | [Index](./SPRING_SECURITY_INDEX.md) | [Next: Part 5 →](./PART_5_ADVANCED_CONCEPTS.md)
