package com.nahid.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahid.gateway.config.GatewaySecurityProperties;
import com.nahid.gateway.dto.CustomAuthContextDto;
import com.nahid.gateway.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.nahid.gateway.util.constant.ExceptionMessageConstant.JWT_TOKEN_EXPIRED;
import static com.nahid.gateway.util.constant.ExceptionMessageConstant.JWT_TOKEN_INVALID;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_CUSTOM_AUTH_CONTEXT = "X-Auth-Context";
    private static final String HEADER_USER_ID = "X-Auth-User";
    private static final String HEADER_USER_ROLES = "X-Auth-Roles";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final GatewaySecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getURI().getPath();

        if (isPublicEndpoint(requestPath)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request.getHeaders());
        if (!StringUtils.hasText(token)) {
            return writeErrorResponse(exchange, "Missing or invalid Authorization header");
        }

        try {
            if (!jwtUtil.isTokenValid(token)) {
                return writeErrorResponse(exchange, JWT_TOKEN_INVALID);
            }

            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);

            CustomAuthContextDto customAuthContextDto = CustomAuthContextDto.builder()
                    .userName(username)
                    .role(roles)
                    .build();

            String customAuthContextDtoJson = objectMapper.writeValueAsString(customAuthContextDto);
            String customAuthContextDtoBase64 = Base64.getEncoder().encodeToString(customAuthContextDtoJson.getBytes());

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(HEADER_CUSTOM_AUTH_CONTEXT, customAuthContextDtoBase64)
                    .build();

//            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                    .header(HEADER_USER_ID, username)
//                    .header(HEADER_USER_ROLES, roles == null ? "" : String.join(",", roles))
//                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired for path {}: {}", requestPath, ex.getMessage());
            return writeErrorResponse(exchange, JWT_TOKEN_EXPIRED);
        } catch (JwtException ex) {
            log.error("JWT validation failed for path {}: {}", requestPath, ex.getMessage());
            return writeErrorResponse(exchange, "JWT validation failed");
        }
        catch (JsonProcessingException e) {
            log.error("Failed to serialize auth context: {}", e.getMessage());
           return writeErrorResponse(exchange, "Failed to serialize auth context");
        }
    }



    private boolean isPublicEndpoint(String path) {
        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String extractToken(HttpHeaders headers) {
        List<String> authorizationHeaders = headers.getOrEmpty(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authorizationHeaders)) {
            return null;
        }

        String authorization = authorizationHeaders.getFirst();
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length());
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> responseBody = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "message", message,
                "path", exchange.getRequest().getURI().getPath()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise error response: {}", e.getMessage());
            byte[] fallback = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

