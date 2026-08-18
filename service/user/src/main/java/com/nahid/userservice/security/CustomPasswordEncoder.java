package com.nahid.userservice.security;


import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomPasswordEncoder implements PasswordEncoder {

    private final AdvancedPasswordHasher passwordHasher;

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return passwordHasher.hashPassword(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordHasher.verifyPassword(rawPassword.toString(), encodedPassword);
    }
}

