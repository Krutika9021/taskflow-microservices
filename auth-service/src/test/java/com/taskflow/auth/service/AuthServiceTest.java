package com.taskflow.auth.service;

import com.taskflow.auth.api.DuplicateEmailException;
import com.taskflow.auth.api.InvalidCredentialsException;
import com.taskflow.auth.domain.Role;
import com.taskflow.auth.domain.Tenant;
import com.taskflow.auth.domain.User;
import com.taskflow.auth.repository.TenantRepository;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Admin@acme.test", "password1", "Acme"))
                .isInstanceOf(DuplicateEmailException.class);

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerMapsUniqueConstraintToDuplicateEmail() {
        Tenant tenant = new Tenant("Acme");
        ReflectionTestUtils.setField(tenant, "id", 1L);
        when(userRepository.existsByEmail("admin@acme.test")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register("admin@acme.test", "password1", "Acme"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginRejectsWrongPasswordWithGenericMessage() {
        User user = new User("admin@acme.test", "hashed", Role.ADMIN, new Tenant("Acme"));
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("admin@acme.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin@acme.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);

        verify(jwtService, never()).generate(any(), any(), any());
    }
}
