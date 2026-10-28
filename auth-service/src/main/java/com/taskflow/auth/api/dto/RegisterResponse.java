package com.taskflow.auth.api.dto;

import com.taskflow.auth.domain.Role;

public record RegisterResponse(Long userId, Long tenantId, String email, Role role) {
}
