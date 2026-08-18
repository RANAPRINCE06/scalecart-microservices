# Spring Security Architecture - Visual Guide

## Complete System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT APPLICATION                                    │
│  (React, Angular, Vue, Mobile App, etc.)                                          │
└────────────────────────┬───────────────────────────────────────────────────────────┘
                         │
                         │ HTTP Request
                         │ Authorization: Bearer <JWT>
                         ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          SPRING BOOT APPLICATION                                    │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │                           FILTER CHAIN                                          │ │
│ │ ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│ │ │  SecurityContextPersistenceFilter                                           │ │ │
│ │ │  LogoutFilter                                                               │ │ │
│ │ │  ┌────────────────────────────────────────────────────────────────────────┐ │ │ │
│ │ │  │  JwtAuthenticationFilter (YOUR CUSTOM)                                 │ │ │ │
│ │ │  │  ┌──────────────────────────────────────────────────────────────────┐  │ │ │ │
│ │ │  │  │ 1. Extract JWT from Authorization header                         │  │ │ │ │
│ │ │  │  │ 2. Validate JWT signature & expiration (JwtService)              │  │ │ │ │
│ │ │  │  │ 3. Load user from DB (UserDetailsService)                        │  │ │ │ │
│ │ │  │  │ 4. Create Authentication object                                  │  │ │ │ │
│ │ │  │  │ 5. Set SecurityContextHolder                                     │  │ │ │ │
│ │ │  │  └──────────────────────────────────────────────────────────────────┘  │ │ │ │
│ │ │  └────────────────────────────────────────────────────────────────────────┘ │ │ │
│ │ │  ExceptionTranslationFilter                                                 │ │ │
│ │ │  FilterSecurityInterceptor (Authorization checks)                           │ │ │
│ │ └─────────────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                                 │
│                                    ▼                                                 │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │                         SECURITY CONTEXT (Thread-Local)                         │ │
│ │  ┌───────────────────────────────────────────────────────────────────────────┐  │ │
│ │  │  Authentication {                                                         │  │ │
│ │  │    principal: User object                                                 │  │ │
│ │  │    authorities: [ROLE_USER]                                               │  │ │
│ │  │    authenticated: true                                                    │  │ │
│ │  │  }                                                                        │  │ │
│ │  └───────────────────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                                 │
│                                    ▼                                                 │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │                          CONTROLLER LAYER                                       │ │
│ │  @RestController                                                                │ │
│ │  @GetMapping("/api/users/me")                                                  │ │
│ │  public UserResponse getMe() { ... }                                           │ │
│ └────────────────────────────┬────────────────────────────────────────────────────┘ │
│                              ▼                                                       │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │                           SERVICE LAYER                                         │ │
│ │  @Service                                                                       │ │
│ │  public class UserService implements UserDetailsService {                      │ │
│ │    loadUserByUsername()  // Load user from DB                                  │ │
│ │    getMe()              // Get current user from SecurityContext               │ │
│ │  }                                                                              │ │
│ └────────────────────────────┬────────────────────────────────────────────────────┘ │
│                              ▼                                                       │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │                         REPOSITORY LAYER                                        │ │
│ │  @Repository                                                                    │ │
│ │  public interface UserRepository extends JpaRepository<User, Long> {           │ │
│ │    Optional<User> findByEmail(String email);                                   │ │
│ │  }                                                                              │ │
│ └────────────────────────────┬────────────────────────────────────────────────────┘ │
│                              ▼                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      DATABASE        │
                    │  ┌────────────────┐  │
                    │  │  users         │  │
                    │  │  refresh_tokens│  │
                    │  └────────────────┘  │
                    └──────────────────────┘
```

---

## Authentication Flow (Login)

```
┌──────────────┐
│ POST /login  │
│ {email, pwd} │
└──────┬───────┘
       │
       ▼
┌────────────────────────────────────────────────┐
│  AuthController                                │
│    ↓                                          │
│  AuthService.login()                          │
│    ↓                                          │
│  Create UsernamePasswordAuthenticationToken   │
│    (email, password, authenticated=false)     │
└────────┬───────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  AuthenticationManager.authenticate()                   │
│    ↓                                                    │
│  Finds DaoAuthenticationProvider                        │
│    ↓                                                    │
│  DaoAuthenticationProvider.authenticate()               │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  UserDetailsService.loadUserByUsername(email)           │
│    ↓                                                    │
│  SELECT * FROM users WHERE email = ?                    │
│    ↓                                                    │
│  Returns User entity (with hashed password)             │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  PasswordEncoder.matches(rawPassword, hashedPassword)   │
│    ↓                                                    │
│  Extract salt from hash                                 │
│  Hash raw password with same salt                       │
│  Compare: computed == stored                            │
│    ↓                                                    │
│  Returns true/false                                     │
└────────┬────────────────────────────────────────────────┘
         │
         ▼ (if true)
┌─────────────────────────────────────────────────────────┐
│  Create authenticated token                             │
│    (user, null, [ROLE_USER], authenticated=true)        │
│    ↓                                                    │
│  Return to AuthService                                  │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  JwtService.generateAccessToken(user)                   │
│    ↓                                                    │
│  Create JWT with claims: {sub, role, iat, exp}          │
│  Sign with secret key (HS256)                           │
│    ↓                                                    │
│  JwtService.generateRefreshToken(user)                  │
│    ↓                                                    │
│  Create JWT with claims: {sub, iat, exp}                │
│    ↓                                                    │
│  Save refresh token to database                         │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  Return AuthResponse {                                  │
│    accessToken: "eyJhbGc...",                           │
│    refreshToken: "eyJhbGc...",                          │
│    tokenType: "Bearer",                                 │
│    expiresIn: 900000                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
```

---

## JWT Request Flow (Authenticated Request)

```
┌────────────────────────────────────┐
│ GET /api/users/me                  │
│ Authorization: Bearer <JWT>        │
└────────┬───────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter                            │
│    ↓                                                │
│  Extract JWT from header                            │
│    ↓                                                │
│  JwtService.extractUsername(jwt)                    │
│    ├─ Parse JWT                                     │
│    ├─ Verify signature                              │
│    ├─ Check expiration                              │
│    └─ Extract "sub" claim → "user@example.com"      │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  UserService.loadUserByUsername("user@example.com") │
│    ↓                                                │
│  SELECT * FROM users WHERE email = ?                │
│    ↓                                                │
│  Returns User entity                                │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  JwtService.isTokenValid(jwt, user)                 │
│    ├─ Username matches? ✅                          │
│    └─ Token not expired? ✅                         │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  Create UsernamePasswordAuthenticationToken         │
│    (user, null, [ROLE_USER], authenticated=true)    │
│    ↓                                                │
│  SecurityContextHolder.getContext()                 │
│    .setAuthentication(authToken)                    │
│                                                     │
│  ✅ USER AUTHENTICATED FOR THIS REQUEST             │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  FilterSecurityInterceptor                          │
│    ├─ Check if authenticated? ✅                    │
│    └─ Check authorization rules? ✅                 │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  UserController.getMe()                             │
│    ↓                                                │
│  UserService.getMe()                                │
│    ↓                                                │
│  User user = SecurityContextHolder                  │
│      .getContext()                                  │
│      .getAuthentication()                           │
│      .getPrincipal();                               │
│    ↓                                                │
│  Return UserResponse                                │
└─────────────────────────────────────────────────────┘
```

---

## Password Journey

```
REGISTRATION:
┌──────────────────┐
│ User enters:     │
│ "SecurePass123"  │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ PasswordEncoder.encode("SecurePass123")     │
│   ↓                                         │
│ Generate random salt                        │
│ Hash password 4096 times (BCrypt strength=12)│
│   ↓                                         │
│ "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkq..."│
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ Database:                                   │
│ users.password =                            │
│   "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9..."   │
└─────────────────────────────────────────────┘

LOGIN:
┌──────────────────┐
│ User enters:     │
│ "SecurePass123"  │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│ PasswordEncoder.matches(                                │
│     "SecurePass123",                    ← Raw password  │
│     "$2a$12$R9h/cIPz0gi..."             ← From DB       │
│ )                                                       │
│   ↓                                                     │
│ Extract salt from stored hash                           │
│ Hash raw password with SAME salt                        │
│ Compare: computed hash == stored hash                   │
│   ↓                                                     │
│ true ✅                                                 │
└─────────────────────────────────────────────────────────┘
```

---

## Token Lifecycle

```
Registration
     ↓
  No tokens
     ↓
  Login ────────────┐
     │              │
     │              ▼
     │   ┌──────────────────────┐
     │   │ Access Token         │
     │   │ Expiry: 15 min       │
     │   │ Claims: role, sub    │
     │   └──────────────────────┘
     │              │
     │              │ Used for API requests
     │              │
     │              ▼
     │   ┌──────────────────────┐
     │   │ Refresh Token        │
     │   │ Expiry: 7 days       │
     │   │ Stored in DB         │
     │   └──────────────────────┘
     │              │
     │              │ Access token expires after 15 min
     │              │
     │              ▼
     │   ┌──────────────────────┐
     │   │ Token Refresh        │
     │   │ - Delete old refresh │
     │   │ - Generate new pair  │
     │   └──────────────────────┘
     │              │
     └──────────────┘
```

---

## Security Configuration Hierarchy

```
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
    │
    ├─ SecurityFilterChain
    │    ├─ CSRF disabled
    │    ├─ CORS configuration
    │    ├─ Authorization rules
    │    ├─ Session management (STATELESS)
    │    ├─ Authentication provider
    │    └─ Custom JWT filter
    │
    ├─ AuthenticationManager
    │    └─ Uses AuthenticationProvider
    │
    ├─ AuthenticationProvider (DaoAuthenticationProvider)
    │    ├─ UserDetailsService
    │    └─ PasswordEncoder
    │
    ├─ PasswordEncoder (BCryptPasswordEncoder)
    │    └─ Strength: 12
    │
    └─ CorsConfigurationSource
         ├─ Allowed origins
         ├─ Allowed methods
         ├─ Allowed headers
         └─ Allow credentials
```

---

## Component Dependencies

```
JwtAuthenticationFilter
    ├─ depends on → JwtService
    │                 └─ Uses JWT secret
    │
    └─ depends on → UserService (UserDetailsService)
                      └─ Uses UserRepository


AuthService
    ├─ depends on → AuthenticationManager
    │                 └─ Uses DaoAuthenticationProvider
    │                       ├─ UserDetailsService
    │                       └─ PasswordEncoder
    │
    ├─ depends on → PasswordEncoder
    │                 └─ BCryptPasswordEncoder
    │
    ├─ depends on → JwtService
    │                 └─ Generate & validate tokens
    │
    └─ depends on → UserRepository
                      └─ Save users & refresh tokens


UserService (implements UserDetailsService)
    └─ depends on → UserRepository
                      └─ Load user by email
```

---

## Thread-Local Security Context

```
Request 1 (User A)          Request 2 (User B)          Request 3 (User C)
     │                           │                           │
     │ Thread 1                  │ Thread 2                  │ Thread 3
     ├──────────────────┐        ├──────────────────┐        ├──────────────────┐
     │ SecurityContext  │        │ SecurityContext  │        │ SecurityContext  │
     │ User: A          │        │ User: B          │        │ User: C          │
     │ Role: USER       │        │ Role: ADMIN      │        │ Role: USER       │
     └──────────────────┘        └──────────────────┘        └──────────────────┘
     │                           │                           │
     ▼                           ▼                           ▼
  Controller                  Controller                  Controller
     │                           │                           │
     ▼                           ▼                           ▼
  Response                    Response                    Response
     │                           │                           │
  Clear context               Clear context               Clear context
```

**Key Point:** Each request runs on its own thread with isolated SecurityContext.

---

## Error Handling Flow

```
Invalid/Missing JWT
     │
     ▼
JwtAuthenticationFilter
     ├─ Catches exceptions
     ├─ Logs error
     └─ Continues filter chain WITHOUT setting authentication
     │
     ▼
FilterSecurityInterceptor
     ├─ Checks authentication
     └─ authentication == null ❌
     │
     ▼
Return 401 Unauthorized
```

---

This visual guide complements the detailed documentation in Parts 1-5. Refer to specific parts for in-depth explanations of each component.

[Back to Index](./SPRING_SECURITY_INDEX.md)
