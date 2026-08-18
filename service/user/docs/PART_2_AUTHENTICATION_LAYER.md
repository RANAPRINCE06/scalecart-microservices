# Part 2: Authentication Layer

## Table of Contents
1. [Authentication Manager](#1-authentication-manager)
2. [Authentication Provider](#2-authentication-provider)
3. [Password Encoder](#3-password-encoder)
4. [User Details Service](#4-user-details-service)
5. [How They Work Together](#5-how-they-work-together)

---

## 1. Authentication Manager

### What is AuthenticationManager?

The **AuthenticationManager** is the main entry point for authentication in Spring Security. It's like a security supervisor who delegates work to specialized officers (Authentication Providers).

**Key Concept**: It doesn't authenticate directly - it **orchestrates** the authentication process.

### Your Configuration

**File**: `SecurityConfig.java` (lines 98-101)
```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
        throws Exception {
    return config.getAuthenticationManager();
}
```

**What this does:**
- Creates a Spring-managed AuthenticationManager bean
- Uses Spring Boot's auto-configuration
- Makes it available for dependency injection

### When is it Used?

**ONLY during LOGIN** - not for JWT validation!

**File**: `AuthService.java` (lines 72-74)
```java
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.getEmail(),      // username
        request.getPassword()    // raw password
    )
);
```

### The authenticate() Method

**Input**: `Authentication` object (unauthenticated)
```java
UsernamePasswordAuthenticationToken(
    principal: "user@example.com",
    credentials: "SecurePass123",
    authenticated: false
)
```

**Process**:
1. Finds appropriate AuthenticationProvider
2. Delegates authentication to that provider
3. Provider validates credentials
4. Returns authenticated `Authentication` object

**Output**: `Authentication` object (authenticated)
```java
UsernamePasswordAuthenticationToken(
    principal: User object,
    credentials: null,  // cleared for security
    authorities: [ROLE_USER],
    authenticated: true
)
```

### What Happens Inside?

```java
// Simplified pseudocode of AuthenticationManager
public Authentication authenticate(Authentication auth) {
    // Step 1: Find matching provider
    for (AuthenticationProvider provider : providers) {
        if (provider.supports(auth.getClass())) {
            
            // Step 2: Attempt authentication
            Authentication result = provider.authenticate(auth);
            
            if (result != null) {
                return result;  // Success!
            }
        }
    }
    
    // Step 3: No provider succeeded
    throw new AuthenticationException("Authentication failed");
}
```

### Important Points ⚠️

**1. Used ONLY for login, not JWT validation**
```java
// ✅ CORRECT - Login flow
authenticationManager.authenticate(token);  // Validates username + password

// ❌ WRONG - JWT validation flow
// JWT filter does NOT use AuthenticationManager
// It manually validates token and sets SecurityContext
```

**2. Requires at least one AuthenticationProvider**
```java
@Bean
public AuthenticationProvider authenticationProvider() {
    // You MUST configure at least one provider
    return new DaoAuthenticationProvider(...);
}
```

**3. Throws exceptions on failure**
```java
try {
    authenticationManager.authenticate(token);
} catch (BadCredentialsException e) {
    // Wrong password
} catch (DisabledException e) {
    // Account disabled
} catch (LockedException e) {
    // Account locked
}
```

---

## 2. Authentication Provider

### What is AuthenticationProvider?

An **AuthenticationProvider** performs the actual authentication logic. Spring provides several implementations:
- **DaoAuthenticationProvider** - Database authentication (you're using this)
- **LdapAuthenticationProvider** - LDAP authentication
- **OAuth2LoginAuthenticationProvider** - OAuth2 authentication

### Your Configuration

**File**: `SecurityConfig.java` (lines 91-95)
```java
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

**What this configures:**
- Creates `DaoAuthenticationProvider` instance
- Injects `UserService` (your UserDetailsService implementation)
- Sets password encoder for password verification
- Returns as Spring bean (registered with AuthenticationManager)

### DaoAuthenticationProvider Process

```
authenticate(UsernamePasswordAuthenticationToken)
    ↓
┌─────────────────────────────────────────────┐
│ Step 1: Load user from database            │
│ userDetails = userDetailsService            │
│               .loadUserByUsername(username) │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│ Step 2: Check if user exists               │
│ if (userDetails == null)                   │
│     throw UsernameNotFoundException        │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│ Step 3: Verify password                    │
│ boolean matches = passwordEncoder           │
│     .matches(rawPassword, hashedPassword)  │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│ Step 4: Check password validity            │
│ if (!matches)                              │
│     throw BadCredentialsException          │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│ Step 5: Check account status               │
│ if (!userDetails.isEnabled())              │
│     throw DisabledException                │
│ if (!userDetails.isAccountNonLocked())     │
│     throw LockedException                  │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│ Step 6: Create authenticated token         │
│ return new UsernamePasswordAuth...Token(  │
│     userDetails,                           │
│     null,  // credentials cleared          │
│     userDetails.getAuthorities()           │
│ )                                          │
└─────────────────────────────────────────────┘
```

### What It Needs

**Two dependencies:**

1. **UserDetailsService** - To load user from database
```java
DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
```

2. **PasswordEncoder** - To verify password
```java
authProvider.setPasswordEncoder(passwordEncoder());
```

### Pseudocode Implementation

```java
// Simplified version of what DaoAuthenticationProvider does
public Authentication authenticate(Authentication auth) {
    String username = auth.getName();
    String password = auth.getCredentials().toString();
    
    // Load user
    UserDetails user = userDetailsService.loadUserByUsername(username);
    
    // Verify password
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new BadCredentialsException("Invalid credentials");
    }
    
    // Check account status
    if (!user.isEnabled()) {
        throw new DisabledException("Account disabled");
    }
    
    // Create authenticated token
    return new UsernamePasswordAuthenticationToken(
        user,
        null,
        user.getAuthorities()
    );
}
```

---

## 3. Password Encoder

### What is PasswordEncoder?

A **PasswordEncoder** provides secure password hashing and verification. It's a one-way encryption - you can't decrypt the hash back to the original password.

### Your Configuration

**File**: `SecurityConfig.java` (lines 84-87)
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength factor = 12
}
```

**BCrypt Strength Levels:**
| Strength | Time to Hash | Security Level | Recommended For |
|----------|--------------|----------------|-----------------|
| 10 | ~100ms | Good | High-traffic sites |
| **12** | **~250ms** | **Better** | **Most applications** (your choice) |
| 14 | ~1000ms | Best | High-security applications |
| 16+ | ~4000ms+ | Overkill | Usually too slow |

### Two Main Methods

#### 1. encode() - Hashing (During Registration)

**File**: `AuthService.java` (line 51)
```java
String hashedPassword = passwordEncoder.encode(request.getPassword());
```

**Example:**
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

String rawPassword = "SecurePass123";
String hash = encoder.encode(rawPassword);

System.out.println(hash);
// Output: $2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqXH8fYr3sK...
```

**Hash Structure:**
```
$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqXH8fYr3sK...
 │  │  │                                           │
 │  │  │                                           └─ Hash (31 chars)
 │  │  └─ Salt (22 chars)
 │  └─ Cost factor (12)
 └─ Algorithm version (2a = BCrypt)
```

**Important**: Same password = Different hash each time!
```java
encoder.encode("password");  // $2a$12$R9h/cIP...
encoder.encode("password");  // $2a$12$T5k3LbK...  (DIFFERENT!)
encoder.encode("password");  // $2a$12$M2n8PqW...  (ALSO DIFFERENT!)
```

**Why?** BCrypt generates a new random salt for each hash.

#### 2. matches() - Verification (During Login)

**Called internally by DaoAuthenticationProvider:**
```java
boolean isValid = passwordEncoder.matches(rawPassword, hashedPasswordFromDB);
```

**Example:**
```java
String rawPassword = "SecurePass123";
String storedHash = "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqXH8fYr3sK...";

boolean result = encoder.matches(rawPassword, storedHash);
// Returns: true

boolean result2 = encoder.matches("WrongPassword", storedHash);
// Returns: false
```

**How it works:**
1. Extract salt from stored hash
2. Hash raw password with same salt
3. Compare generated hash with stored hash
4. Return true if identical

### Security Features

**1. Salt Protection**
```
Without salt:
password123 → 5f4dcc3b5aa765d61d8327deb882cf99 (always same)
              ⚠️ Vulnerable to rainbow tables

With salt (BCrypt):
password123 → $2a$12$R9h/cIPz... (unique each time)
password123 → $2a$12$T5k3LbK... (different!)
              ✅ Rainbow tables useless
```

**2. Computational Cost**
```
Strength 12 = 2^12 = 4,096 iterations
- Fast enough for legitimate users (~250ms)
- Slow enough to deter brute force attacks
- Attacker must spend 250ms per guess
```

**3. One-Way Encryption**
```java
// ✅ Possible
String hash = encoder.encode("password");

// ❌ IMPOSSIBLE - no decrypt method
String original = encoder.decode(hash);  // Does not exist!
```

### Real-World Example

**Registration (User creates account):**
```java
// User submits: password = "MySecurePass123"
String hashedPassword = passwordEncoder.encode("MySecurePass123");
// Result: $2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkq...

// Save to database
user.setPassword(hashedPassword);
userRepository.save(user);
```

**Database stores:**
```
+----+-------------------+--------------------------------------+
| id | email             | password                             |
+----+-------------------+--------------------------------------+
| 1  | user@example.com  | $2a$12$R9h/cIPz0gi.URNNX3kh2... |
+----+-------------------+--------------------------------------+
```

**Login (User tries to login):**
```java
// User submits: password = "MySecurePass123"
User userFromDB = userRepository.findByEmail("user@example.com");

boolean matches = passwordEncoder.matches(
    "MySecurePass123",                           // Raw password from login form
    "$2a$12$R9h/cIPz0gi.URNNX3kh2..."           // Hashed password from database
);
// Returns: true ✅

// User submits wrong password
boolean matches2 = passwordEncoder.matches(
    "WrongPassword",
    "$2a$12$R9h/cIPz0gi.URNNX3kh2..."
);
// Returns: false ❌
```

---

## 4. User Details Service

### What is UserDetailsService?

**UserDetailsService** is the bridge between Spring Security and your database. It has ONE job: load user information by username.

**Interface definition:**
```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

### Your Implementation

**File**: `UserService.java` (lines 30, 36-44)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    String.format(ExceptionMessageConstant.ENTITY_NOT_FOUND_BY_FIELD,
                        AppConstant.USER, "email", username)));
    }
}
```

### When is it Called?

**Two scenarios:**

**1. During Login (by DaoAuthenticationProvider)**
```java
// AuthService.java - login()
authenticationManager.authenticate(...)
    ↓
DaoAuthenticationProvider.authenticate()
    ↓
userService.loadUserByUsername("user@example.com")  // ← CALLED HERE
    ↓
Database query: SELECT * FROM users WHERE email = 'user@example.com'
```

**2. During JWT Validation (by JwtAuthenticationFilter)**
```java
// JwtAuthenticationFilter.java (line 58)
UserDetails userDetails = userService.loadUserByUsername(userEmail);
    ↓
Database query: SELECT * FROM users WHERE email = 'user@example.com'
```

### What It Returns?

Returns a **UserDetails** object, which is an interface with these methods:

```java
public interface UserDetails {
    String getUsername();                               // Unique identifier (email in your case)
    String getPassword();                               // Hashed password
    Collection<? extends GrantedAuthority> getAuthorities();  // Roles/permissions
    boolean isAccountNonExpired();                      // Account validity
    boolean isAccountNonLocked();                       // Account lockout status
    boolean isCredentialsNonExpired();                  // Password validity
    boolean isEnabled();                                // Account active status
}
```

### Your User Entity

**File**: `User.java` (line 26)
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity<Long> implements UserDetails {
    
    private String email;
    private String password;  // Already hashed
    private String firstName;
    private String lastName;
    private Role role;
    
    @Override
    public String getUsername() {
        return this.email;  // Email is the username
    }
    
    @Override
    public String getPassword() {
        return this.password;  // Already hashed in database
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        // Returns: ["ROLE_USER"] or ["ROLE_ADMIN"]
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;  // true by default
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;  // true by default
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;  // true by default
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;  // true by default
    }
}
```

### Important Notes ⚠️

**1. Username can be any unique field**
```java
// ✅ Email as username (your implementation)
public String getUsername() {
    return this.email;
}

// ✅ Actual username field (alternative)
public String getUsername() {
    return this.username;
}

// ✅ Phone number (alternative)
public String getUsername() {
    return this.phoneNumber;
}
```

**2. Password must be hashed**
```java
// ✅ CORRECT - Return hashed password
public String getPassword() {
    return this.password;  // "$2a$12$R9h/cIPz..."
}

// ❌ WRONG - Never return raw password
public String getPassword() {
    return "plaintextPassword";
}
```

**3. Throws exception if user not found**
```java
return userRepository.findByEmail(username)
    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
```

**What happens:**
- DaoAuthenticationProvider catches this
- Converts to `BadCredentialsException`
- User sees "Invalid credentials" (security best practice - don't reveal if email exists)

### Database Query Example

When you call `loadUserByUsername("user@example.com")`:

```sql
SELECT * FROM users WHERE email = 'user@example.com';
```

**Returns:**
```
+----+-------------------+-------------------------------------+-----------+----------+--------+
| id | email             | password                            | first_name| last_name| role   |
+----+-------------------+-------------------------------------+-----------+----------+--------+
| 1  | user@example.com  | $2a$12$R9h/cIPz0gi.URNNX3kh2...   | John      | Doe      | USER   |
+----+-------------------+-------------------------------------+-----------+----------+--------+
```

**Mapped to User entity:**
```java
User {
    id: 1,
    email: "user@example.com",
    password: "$2a$12$R9h/cIPz0gi.URNNX3kh2...",
    firstName: "John",
    lastName: "Doe",
    role: Role.USER,
    authorities: [SimpleGrantedAuthority("ROLE_USER")]
}
```

---

## 5. How They Work Together

### Login Authentication Flow

```
┌──────────────────────────────────────────────────────────────┐
│ User Login Request                                           │
│ POST /api/auth/login                                         │
│ { "email": "user@example.com", "password": "SecurePass123" } │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  AuthService.login()           │
        │  (Lines 68-78)                 │
        └────────────┬───────────────────┘
                     │
                     │ Creates token
                     ▼
        ┌─────────────────────────────────────────────┐
        │  UsernamePasswordAuthenticationToken        │
        │  - principal: "user@example.com"            │
        │  - credentials: "SecurePass123"             │
        │  - authenticated: false                     │
        └────────────┬────────────────────────────────┘
                     │
                     │ authenticate()
                     ▼
        ┌─────────────────────────────────────────────┐
        │  AuthenticationManager                      │
        │  (SecurityConfig lines 98-101)              │
        │  "Who can authenticate this?"               │
        └────────────┬────────────────────────────────┘
                     │
                     │ Delegates to
                     ▼
        ┌─────────────────────────────────────────────┐
        │  DaoAuthenticationProvider                  │
        │  (SecurityConfig lines 91-95)               │
        │  "I can handle username/password!"          │
        └────────────┬────────────────────────────────┘
                     │
                     │ Load user
                     ▼
        ┌─────────────────────────────────────────────┐
        │  UserDetailsService                         │
        │  (UserService lines 37-44)                  │
        │                                             │
        │  loadUserByUsername("user@example.com")     │
        │      ↓                                      │
        │  userRepository.findByEmail()               │
        │      ↓                                      │
        │  Returns User entity                        │
        └────────────┬────────────────────────────────┘
                     │
                     │ User loaded
                     ▼
        ┌─────────────────────────────────────────────┐
        │  Back to DaoAuthenticationProvider          │
        │                                             │
        │  User user = ...                            │
        │  Now verify password ↓                      │
        └────────────┬────────────────────────────────┘
                     │
                     │ Verify password
                     ▼
        ┌─────────────────────────────────────────────┐
        │  PasswordEncoder                            │
        │  (BCryptPasswordEncoder)                    │
        │                                             │
        │  matches(                                   │
        │      "SecurePass123",           ← Raw       │
        │      "$2a$12$R9h/cIPz..."       ← Hash      │
        │  )                                          │
        │  → Returns true ✅                          │
        └────────────┬────────────────────────────────┘
                     │
                     │ Password valid!
                     ▼
        ┌─────────────────────────────────────────────┐
        │  DaoAuthenticationProvider                  │
        │  Creates authenticated token                │
        │                                             │
        │  return new UsernamePasswordAuth...Token(   │
        │      user,              ← User object       │
        │      null,              ← Credentials clear │
        │      user.getAuthorities()  ← [ROLE_USER]  │
        │  )                                          │
        └────────────┬────────────────────────────────┘
                     │
                     │ Authenticated!
                     ▼
        ┌─────────────────────────────────────────────┐
        │  Back to AuthService                        │
        │  (Line 75)                                  │
        │                                             │
        │  User user = (User) authentication          │
        │                   .getPrincipal();          │
        │                                             │
        │  Generate JWT tokens                        │
        │  Return to client                           │
        └─────────────────────────────────────────────┘
```

### Component Interaction Table

| Step | Component | Input | Action | Output |
|------|-----------|-------|--------|--------|
| 1 | **AuthenticationManager** | Username + Password | Find provider | Delegates to provider |
| 2 | **AuthenticationProvider** | Username + Password | Start authentication | Calls UserDetailsService |
| 3 | **UserDetailsService** | Username | Query database | Returns User entity |
| 4 | **AuthenticationProvider** | User + Password | Verify password | Calls PasswordEncoder |
| 5 | **PasswordEncoder** | Raw + Hashed | Compare | true/false |
| 6 | **AuthenticationProvider** | Validation result | Create token | Authenticated token |
| 7 | **AuthenticationManager** | Authenticated token | Return result | To caller (AuthService) |

---

## Key Takeaways 🎯

1. **AuthenticationManager** orchestrates - it doesn't authenticate directly
2. **AuthenticationProvider** does the actual work - uses UserDetailsService + PasswordEncoder
3. **PasswordEncoder** is cryptographically secure - one-way, salted, adjustable cost
4. **UserDetailsService** is your database adapter - bridges Spring Security to your data
5. **Used only during LOGIN** - JWT validation bypasses this entire flow

---

## Next Steps

Continue to **[Part 3: Registration & Login Flows](./PART_3_REGISTRATION_LOGIN_FLOWS.md)** to see complete request flows with every step detailed.

---

[← Back to Part 1](./PART_1_CORE_COMPONENTS.md) | [Index](./SPRING_SECURITY_INDEX.md) | [Next: Part 3 →](./PART_3_REGISTRATION_LOGIN_FLOWS.md)
