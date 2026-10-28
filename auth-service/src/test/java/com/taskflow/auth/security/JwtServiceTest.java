package com.taskflow.auth.security;

import com.taskflow.auth.domain.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "local-dev-only-change-me-32bytes-min";
    private static final String OTHER_SECRET = "other-secret-must-be-32-bytes-min!";
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ONE_HOUR_MS, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void generateAndParseRoundTripsClaims() {
        String token = jwtService.generate(42L, 7L, Role.ADMIN);

        JwtPrincipal principal = jwtService.parse(token);

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.tenantId()).isEqualTo(7L);
        assertThat(principal.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void parseRejectsExpiredToken() {
        String token = jwtService.generate(1L, 1L, Role.USER);
        JwtService later = new JwtService(
                SECRET,
                ONE_HOUR_MS,
                Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> later.parse(token))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtService.generate(1L, 1L, Role.USER);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRejectsTokenSignedWithWrongSecret() {
        String token = jwtService.generate(1L, 1L, Role.USER);
        JwtService other = new JwtService(OTHER_SECRET, ONE_HOUR_MS, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> other.parse(token))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRejectsUnknownRole() {
        String token = Jwts.builder()
                .claim(JwtService.CLAIM_USER_ID, 1L)
                .claim(JwtService.CLAIM_TENANT_ID, 1L)
                .claim(JwtService.CLAIM_ROLE, "SUPERADMIN")
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plusMillis(ONE_HOUR_MS)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(InvalidJwtException.class);
    }
}
