package com.taskflow.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthGatewayFilterTest {

    private static final String SECRET = "local-dev-only-change-me-32bytes-min";
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;
    private JwtAuthGatewayFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ONE_HOUR_MS, Clock.fixed(NOW, ZoneOffset.UTC));
        filter = new JwtAuthGatewayFilter(jwtService);
    }

    @Test
    void validTokenSetsIdentityHeadersFromClaims() {
        String token = jwtService.generate(42L, 7L, Role.ADMIN);
        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );
        GatewayFilterChain chain = ex -> {
            captured.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(captured.get().getHeaders().getFirst(JwtAuthGatewayFilter.HEADER_USER_ID)).isEqualTo("42");
        assertThat(captured.get().getHeaders().getFirst(JwtAuthGatewayFilter.HEADER_TENANT_ID)).isEqualTo("7");
        assertThat(captured.get().getHeaders().getFirst(JwtAuthGatewayFilter.HEADER_ROLE)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenReturns401() {
        String token = jwtService.generate(1L, 1L, Role.USER);
        JwtAuthGatewayFilter later = new JwtAuthGatewayFilter(
                new JwtService(SECRET, ONE_HOUR_MS, Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC))
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        later.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingTokenReturns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/tasks").build());

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void spoofedIdentityHeadersAreReplacedFromToken() {
        String token = jwtService.generate(42L, 7L, Role.USER);
        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(JwtAuthGatewayFilter.HEADER_USER_ID, "999")
                        .header(JwtAuthGatewayFilter.HEADER_TENANT_ID, "888")
                        .header(JwtAuthGatewayFilter.HEADER_ROLE, "ADMIN")
                        .build()
        );
        GatewayFilterChain chain = ex -> {
            captured.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(captured.get().getHeaders().get(JwtAuthGatewayFilter.HEADER_USER_ID)).containsExactly("42");
        assertThat(captured.get().getHeaders().get(JwtAuthGatewayFilter.HEADER_TENANT_ID)).containsExactly("7");
        assertThat(captured.get().getHeaders().get(JwtAuthGatewayFilter.HEADER_ROLE)).containsExactly("USER");
    }
}
