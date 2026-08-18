package com.nahid.userservice.service;

import com.nahid.userservice.dto.request.AuthRequest;
import com.nahid.userservice.dto.request.RegisterRequest;
import com.nahid.userservice.dto.response.AuthResponse;
import com.nahid.userservice.dto.response.RegisterResponse;
import com.nahid.userservice.entity.RefreshToken;
import com.nahid.userservice.entity.User;
import com.nahid.userservice.enums.Role;
import com.nahid.userservice.exception.AuthenticationException;
import com.nahid.userservice.repository.RefreshTokenRepository;
import com.nahid.userservice.repository.UserRepository;
import com.nahid.userservice.util.annotation.Auditable;
import com.nahid.userservice.util.constant.ExceptionMessageConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.nahid.userservice.util.constant.AppConstant.USER;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value( "${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Transactional
    @Auditable(eventType = "CREATE", entityName = USER, action = "REGISTER_USER")
    public RegisterResponse register(RegisterRequest request) throws AuthenticationException {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException(ExceptionMessageConstant.EMAIL_ALREADY_REGISTERED);
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return RegisterResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole())
                .build();
    }

    @Transactional
    @Auditable(eventType = "ACCESS", entityName = USER, action = "LOGIN_USER")
    public AuthResponse login(AuthRequest request) {

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(request.getEmail(),
                        request.getPassword());
        Authentication authentication = authenticationManager.authenticate(
                token
        );
        User user = (User) authentication.getPrincipal();

        return generateTokenAndResponse(user);
    }

    @Transactional
    @Auditable(eventType = "ACCESS", entityName = USER, action = "REFRESH_TOKEN")
    public AuthResponse refreshToken(String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Invalid authorization header format. Bearer token required");
        }

        String token = authHeader.substring(7);
        if (token.trim().isEmpty()) {
            throw new AuthenticationException("Refresh token cannot be empty");
        }

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() -> {
                    return new AuthenticationException(ExceptionMessageConstant.INVALID_REFRESH_TOKEN);
                });

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException(ExceptionMessageConstant.REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException(ExceptionMessageConstant.REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);

        return generateTokenAndResponse(user);

    }

    private AuthResponse generateTokenAndResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration) // 15 minutes in seconds
                .build();
    }


}