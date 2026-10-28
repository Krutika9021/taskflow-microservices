package com.taskflow.gateway.security;

public record JwtPrincipal(Long userId, Long tenantId, Role role) {
}
