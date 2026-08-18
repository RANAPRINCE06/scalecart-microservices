# Spring Security Deep Dive - Complete Guide

## 📚 Documentation Index

This comprehensive guide explains Spring Security architecture and how it works in your microservice codebase.

### **Quick Navigation**

📋 **[Quick Reference Guide](./QUICK_REFERENCE.md)** - Cheat sheet for common patterns & code snippets

🏗️ **[Architecture Diagrams](./ARCHITECTURE_DIAGRAM.md)** - Visual system architecture & flow diagrams

---

### **Detailed Documentation**

1. **[Part 1: Core Components](./PART_1_CORE_COMPONENTS.md)** ⭐ Start Here
   - Filter & Filter Chain
   - Authentication Filter (JwtAuthenticationFilter)
   - Security Context & SecurityContextHolder
   - How components interact

2. **[Part 2: Authentication Layer](./PART_2_AUTHENTICATION_LAYER.md)**
   - Authentication Manager
   - Authentication Provider
   - Password Encoder (BCrypt)
   - UserDetailsService
   - How login authentication works

3. **[Part 3: Registration & Login Flows](./PART_3_REGISTRATION_LOGIN_FLOWS.md)**
   - Complete registration process step-by-step
   - Complete login authentication flow
   - JWT token generation
   - Database interactions

4. **[Part 4: JWT Request Flow](./PART_4_JWT_REQUEST_FLOW.md)**
   - How authenticated requests work
   - JWT validation process
   - SecurityContext population
   - Request authorization

5. **[Part 5: Advanced Concepts](./PART_5_ADVANCED_CONCEPTS.md)**
   - Session management (stateless)
   - CORS configuration
   - Method security (@PreAuthorize)
   - Common security pitfalls
   - Best practices

---

## 🎯 Learning Path

### For Complete Beginners
1. Read Part 1 (Core Components) - understand the building blocks
2. Read Part 3 (Flows) - see how registration and login work
3. Read Part 2 (Authentication Layer) - dive deeper into authentication
4. Read Part 4 (JWT Flow) - understand request handling
5. Read Part 5 (Advanced) - learn best practices

### For Quick Reference
- **"How does login work?"** → Part 3
- **"How does JWT validation work?"** → Part 4
- **"What is SecurityContext?"** → Part 1
- **"What does PasswordEncoder do?"** → Part 2
- **"How to secure endpoints?"** → Part 5

---

## 🔑 Key Concepts at a Glance

| Component | Purpose | When Used | Your Implementation |
|-----------|---------|-----------|---------------------|
| **Filter Chain** | Intercepts all requests | Every request | 15+ Spring filters + custom JWT filter |
| **JwtAuthenticationFilter** | Validates JWT tokens | Secured endpoints | `JwtAuthenticationFilter.java` |
| **SecurityContext** | Stores current user | During request | `SecurityContextHolder` |
| **AuthenticationManager** | Orchestrates login | Login only | `SecurityConfig.authenticationManager()` |
| **AuthenticationProvider** | Validates credentials | Login only | `DaoAuthenticationProvider` |
| **PasswordEncoder** | Hashes/verifies passwords | Register & Login | `BCryptPasswordEncoder(12)` |
| **UserDetailsService** | Loads user from DB | Login & JWT validation | `UserService` |

---

## 🔄 Quick Flow Diagrams

### Registration Flow (Simplified)
```
Client → Controller → AuthService → PasswordEncoder.encode() → Database
                                          ↓
                                    Store hashed password
```

### Login Flow (Simplified)
```
Client → Controller → AuthService → AuthenticationManager
                                          ↓
                                    AuthenticationProvider
                                          ↓
                                    UserDetailsService (load user)
                                          ↓
                                    PasswordEncoder.matches() (verify)
                                          ↓
                                    Generate JWT tokens
                                          ↓
                                    Return to client
```

### Authenticated Request Flow (Simplified)
```
Client (with JWT) → JwtAuthenticationFilter → Validate JWT
                                                    ↓
                                              Load UserDetails
                                                    ↓
                                              Set SecurityContext
                                                    ↓
                                              Controller (authorized)
```

---

## 📂 Your Implementation Files

### Security Configuration
- `SecurityConfig.java` - Main security setup, beans, filter chain
- `JwtAuthenticationFilter.java` - Custom JWT validation filter

### Authentication Services
- `AuthService.java` - Login, register, token refresh logic
- `UserService.java` - UserDetailsService implementation
- `JwtService.java` - JWT token generation and validation

### Entities
- `User.java` - User entity implementing UserDetails
- `RefreshToken.java` - Refresh token storage

### Password Handling
- `BCryptPasswordEncoder` - Default password encoder (strength 12)
- `CustomPasswordEncoder.java` - Alternative custom implementation
- `AdvancedPasswordHasher.java` - Custom hashing algorithm

---

## 🚨 Common Questions Answered

**Q: Why do we need both access token and refresh token?**
- Access token: Short-lived (15 min), used for API requests
- Refresh token: Long-lived (7 days), used to get new access tokens
- Security: If access token stolen, expires quickly

**Q: When is AuthenticationManager used vs JwtAuthenticationFilter?**
- **AuthenticationManager**: Used ONLY during login (username/password validation)
- **JwtAuthenticationFilter**: Used for ALL requests with JWT tokens

**Q: Why is password stored as hash in database?**
- Security: Even if database is compromised, passwords cannot be decrypted
- BCrypt: One-way algorithm, computationally expensive to crack

**Q: What is SecurityContext and why is it important?**
- Thread-local storage holding current authenticated user
- Accessible anywhere in your code via `SecurityContextHolder`
- Cleared automatically after request completes

**Q: Why implement UserDetails in User entity?**
- Spring Security requires UserDetails interface
- Provides authentication info: username, password, authorities, account status
- Your User entity becomes Spring Security compatible

---

## 🛠️ Hands-On Exercises

After reading the documentation, try these exercises:

1. **Trace a Request**: Add log statements in `JwtAuthenticationFilter` to see when it executes
2. **Custom Role**: Add a new role (ADMIN) and secure an endpoint with it
3. **Token Expiration**: Change access token expiry to 5 minutes and test
4. **Custom Claim**: Add "userId" claim to JWT access token
5. **Failed Login**: Test login with wrong password and observe the exception flow

---

## 📖 Reading Order

### Linear Approach (Recommended)
Read parts 1 → 2 → 3 → 4 → 5 in order

### Practical Approach
1. Part 3 (see the flows first)
2. Part 1 (understand components)
3. Part 2 (deep dive authentication)
4. Part 4 (JWT handling)
5. Part 5 (advanced topics)

---

## 🔗 External Resources

- [Spring Security Official Docs](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/) - Decode and inspect JWT tokens
- [BCrypt Calculator](https://bcrypt-generator.com/) - Test BCrypt hashing
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture)

---

## 💡 Next Steps

1. **Start with [Part 1: Core Components](./PART_1_CORE_COMPONENTS.md)**
2. Follow the learning path above
3. Reference your actual code files alongside the documentation
4. Try the hands-on exercises

**Note**: All code references point to your actual implementation in the `user-service` module.
