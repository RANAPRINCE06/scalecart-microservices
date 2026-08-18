# Part 3: Registration & Login Flows

## Table of Contents
1. [Registration Flow](#1-registration-flow)
2. [Login Flow](#2-login-flow)
3. [Token Refresh Flow](#3-token-refresh-flow)
4. [Comparison Summary](#4-comparison-summary)

---

## 1. Registration Flow

### Overview
Creates new user account with hashed password. **No authentication** - user must login afterward.

### Step-by-Step Process

```
POST /api/auth/register
{ "email": "john@example.com", "password": "SecurePass123", ... }
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ 1. FILTER CHAIN                                          │
│    - JwtAuthenticationFilter skips (public endpoint)     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 2. AuthController.register()                             │
│    - Delegates to AuthService                            │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 3. AuthService.register() (Lines 44-65)                  │
│    - Check email uniqueness                              │
│    - Build User entity                                   │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 4. PASSWORD HASHING                                      │
│    passwordEncoder.encode("SecurePass123")               │
│    Input:  "SecurePass123"                               │
│    Output: "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkq..."  │
│                                                          │
│    Process: Generate salt → Hash 4096 times → Combine   │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 5. DATABASE INSERT                                       │
│    INSERT INTO users (email, password, ...)             │
│    VALUES ('john@example.com', '$2a$12$R9h...', ...)    │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 6. RESPONSE                                              │
│    { "id": 1, "email": "john@example.com", ... }         │
│    ⚠️ NO JWT TOKEN - Must login next                     │
└──────────────────────────────────────────────────────────┘
```

### Components Usage

| Component | Used? | Purpose |
|-----------|-------|---------|
| PasswordEncoder | ✅ Yes | Hash password before storage |
| AuthenticationManager | ❌ No | Not validating credentials |
| UserDetailsService | ❌ No | Not loading existing user |
| JwtService | ❌ No | Not generating tokens |

---

## 2. Login Flow

### Overview
Validates credentials and generates JWT tokens. **ALL authentication components** used here.

### Complete Authentication Flow

```
POST /api/auth/login
{ "email": "john@example.com", "password": "SecurePass123" }
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ 1. FILTER CHAIN                                          │
│    - JwtAuthenticationFilter skips (public endpoint)     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 2. AuthService.login() - Create Token (Lines 70-71)     │
│    UsernamePasswordAuthenticationToken token =           │
│        new UsernamePasswordAuthenticationToken(          │
│            "john@example.com",  // principal             │
│            "SecurePass123"      // credentials           │
│        );                                                │
│    Status: authenticated = false ❌                      │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 3. AUTHENTICATION MANAGER (Line 72)                      │
│    authenticationManager.authenticate(token)             │
│    Task: Find appropriate provider                       │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 4. DAO AUTHENTICATION PROVIDER                           │
│    Step 4.1: Load user from database                     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 5. USER DETAILS SERVICE (Lines 37-44)                    │
│    userService.loadUserByUsername("john@example.com")    │
│    │                                                     │
│    └─> SELECT * FROM users WHERE email = 'john@...'     │
│    │                                                     │
│    Returns: User { password: "$2a$12$R9h...", ... }     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 6. PASSWORD ENCODER - Verification                       │
│    passwordEncoder.matches(                              │
│        "SecurePass123",              ← Raw from request  │
│        "$2a$12$R9h/cIPz0gi..."      ← Hash from DB      │
│    )                                                     │
│    │                                                     │
│    Process:                                              │
│    1. Extract salt: "R9h/cIPz0gi.URNNX3kh2"             │
│    2. Hash raw password with same salt                   │
│    3. Compare: computed hash == stored hash              │
│    │                                                     │
│    Result: true ✅                                       │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 7. CREATE AUTHENTICATED TOKEN                            │
│    new UsernamePasswordAuthenticationToken(              │
│        user,                  // User object             │
│        null,                  // Clear credentials       │
│        [ROLE_USER]            // Authorities             │
│    )                                                     │
│    Status: authenticated = true ✅                       │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 8. GENERATE JWT TOKENS (Lines 115-133)                  │
│    │                                                     │
│    ├─ Access Token (15 min expiry)                      │
│    │  Claims: { "role": ["ROLE_USER"] }                 │
│    │  Subject: "john@example.com"                        │
│    │  Signature: HS256                                   │
│    │  Result: "eyJhbGciOiJIUzI1NiJ9..."                 │
│    │                                                     │
│    └─ Refresh Token (7 days expiry)                     │
│       No extra claims                                    │
│       Result: "eyJhbGciOiJIUzI1NiJ9..."                 │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 9. SAVE REFRESH TOKEN (Lines 119-125)                   │
│    INSERT INTO refresh_tokens (token, user_id, ...)     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 10. RESPONSE                                             │
│     {                                                    │
│       "accessToken": "eyJhbGci...",                      │
│       "refreshToken": "eyJhbGci...",                     │
│       "tokenType": "Bearer",                             │
│       "expiresIn": 900000                                │
│     }                                                    │
└──────────────────────────────────────────────────────────┘
```

### Components Usage

| Component | Used? | Purpose |
|-----------|-------|---------|
| AuthenticationManager | ✅ Yes | Orchestrate authentication |
| DaoAuthenticationProvider | ✅ Yes | Perform actual authentication |
| UserDetailsService | ✅ Yes | Load user from database |
| PasswordEncoder | ✅ Yes | Verify password |
| JwtService | ✅ Yes | Generate access & refresh tokens |

### JWT Token Structure

**Access Token Decoded:**
```json
{
  "sub": "john@example.com",
  "role": ["ROLE_USER"],
  "iat": 1698765432,
  "exp": 1698766332
}
```

**Refresh Token Decoded:**
```json
{
  "sub": "john@example.com",
  "iat": 1698765432,
  "exp": 1699370232
}
```

---

## 3. Token Refresh Flow

### Overview
Generates new access token using valid refresh token without re-entering password.

### Step-by-Step Process

```
POST /api/auth/refresh
Header: Authorization: Bearer <refresh_token>
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ 1. AuthService.refreshToken() (Lines 81-112)            │
│    - Extract refresh token from header                   │
│    - Validate format                                     │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 2. FIND REFRESH TOKEN IN DATABASE                       │
│    refreshTokenRepository.findByToken(token)             │
│    │                                                     │
│    Checks:                                               │
│    ├─ Token exists? ✅                                   │
│    ├─ Not revoked? ✅                                    │
│    └─ Not expired? ✅                                    │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 3. DELETE OLD REFRESH TOKEN                             │
│    refreshTokenRepository.delete(refreshToken)           │
│    (Security: One-time use)                              │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 4. GENERATE NEW TOKENS                                   │
│    - New access token (15 min)                           │
│    - New refresh token (7 days)                          │
│    - Save new refresh token to DB                        │
└──────────────────┬───────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│ 5. RESPONSE                                              │
│    { "accessToken": "...", "refreshToken": "..." }       │
└──────────────────────────────────────────────────────────┘
```

### Key Points
- ❌ **NO password validation** - uses existing refresh token
- ❌ **NO AuthenticationManager** - not validating credentials
- ✅ **YES database check** - refresh token must exist and be valid
- ✅ **Refresh token rotation** - old token deleted, new one issued

---

## 4. Comparison Summary

### Registration vs Login vs Token Refresh

| Aspect | Registration | Login | Token Refresh |
|--------|-------------|-------|---------------|
| **Endpoint** | `/api/auth/register` | `/api/auth/login` | `/api/auth/refresh` |
| **Input** | User details + password | Email + password | Refresh token |
| **AuthenticationManager** | ❌ No | ✅ Yes | ❌ No |
| **UserDetailsService** | ❌ No | ✅ Yes | ❌ No |
| **PasswordEncoder.encode()** | ✅ Yes (hash) | ❌ No | ❌ No |
| **PasswordEncoder.matches()** | ❌ No | ✅ Yes (verify) | ❌ No |
| **Database Write** | User table | Refresh token table | Refresh token table |
| **Database Read** | Check email exists | Read user | Read refresh token |
| **JWT Generation** | ❌ No | ✅ Yes | ✅ Yes |
| **Output** | User details | Access + Refresh tokens | Access + Refresh tokens |
| **SecurityContext** | ❌ Not set | ❌ Not set (public) | ❌ Not set (public) |

### Password Journey

**Registration:**
```
User enters → "SecurePass123"
              ↓ encode()
Database stores → "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9..."
```

**Login:**
```
User enters → "SecurePass123"
              ↓
              matches("SecurePass123", "$2a$12$R9h...")
              ↓
Database reads → "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9..."
              ↓
Result → true ✅ (passwords match)
```

### Token Lifecycle

```
Registration
    ↓ (No tokens)
Login
    ↓ (Generate tokens)
    ├─ Access Token (15 min) ────┐
    │                            │ Use for API requests
    └─ Refresh Token (7 days) ───┘
              ↓ (Access token expires)
Token Refresh
    ↓ (Generate new tokens)
    ├─ New Access Token (15 min)
    └─ New Refresh Token (7 days)
```

---

## Key Takeaways 🎯

1. **Registration** = Simple CRUD + password hashing
2. **Login** = Full authentication chain + JWT generation
3. **Token Refresh** = Database validation + new token generation
4. **Password always hashed** - Never stored or compared in plain text
5. **JWT tokens are stateless** - No server-side session storage
6. **Refresh token in DB** - Allows revocation and security tracking

---

## Next Steps

Continue to **[Part 4: JWT Request Flow](./PART_4_JWT_REQUEST_FLOW.md)** to see how authenticated requests are processed.

---

[← Back to Part 2](./PART_2_AUTHENTICATION_LAYER.md) | [Index](./SPRING_SECURITY_INDEX.md) | [Next: Part 4 →](./PART_4_JWT_REQUEST_FLOW.md)
