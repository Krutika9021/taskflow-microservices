package com.taskflow.auth.service;

import com.taskflow.auth.api.DuplicateEmailException;
import com.taskflow.auth.api.InvalidCredentialsException;
import com.taskflow.auth.api.dto.LoginResponse;
import com.taskflow.auth.api.dto.RegisterResponse;
import com.taskflow.auth.domain.Role;
import com.taskflow.auth.domain.Tenant;
import com.taskflow.auth.domain.User;
import com.taskflow.auth.repository.TenantRepository;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.auth.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(String email, String rawPassword, String tenantName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        try {
            Tenant tenant = tenantRepository.save(new Tenant(tenantName.trim()));
            User user = userRepository.save(new User(
                    normalizedEmail,
                    passwordEncoder.encode(rawPassword),
                    Role.ADMIN,
                    tenant
            ));
            return new RegisterResponse(user.getId(), tenant.getId(), user.getEmail(), user.getRole());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generate(user.getId(), user.getTenant().getId(), user.getRole());
        return new LoginResponse(token);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
