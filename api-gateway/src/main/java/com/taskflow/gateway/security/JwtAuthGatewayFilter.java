package com.taskflow.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_ROLE = "X-Role";

    private final JwtService jwtService;

    public JwtAuthGatewayFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isAuthPath(path)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        try {
            JwtPrincipal principal = jwtService.parse(authorization.substring(7).trim());
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(HEADER_USER_ID);
                        headers.remove(HEADER_TENANT_ID);
                        headers.remove(HEADER_ROLE);
                        headers.set(HEADER_USER_ID, String.valueOf(principal.userId()));
                        headers.set(HEADER_TENANT_ID, String.valueOf(principal.tenantId()));
                        headers.set(HEADER_ROLE, principal.role().name());
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (InvalidJwtException ex) {
            return unauthorized(exchange);
        }
    }

    private static boolean isAuthPath(String path) {
        return "/auth".equals(path) || path.startsWith("/auth/");
    }

    private static ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_TENANT_ID);
                    headers.remove(HEADER_ROLE);
                })
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
