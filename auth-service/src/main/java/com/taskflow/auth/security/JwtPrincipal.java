package com.taskflow.auth.security;

import com.taskflow.auth.domain.Role;

public record JwtPrincipal(Long userId, Long tenantId, Role role) {
}
