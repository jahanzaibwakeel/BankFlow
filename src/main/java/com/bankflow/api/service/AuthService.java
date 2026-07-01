package com.bankflow.api.service;

import com.bankflow.api.domain.AuditAction;
import com.bankflow.api.domain.RefreshToken;
import com.bankflow.api.domain.Role;
import com.bankflow.api.domain.User;
import com.bankflow.api.dto.AuthDtos.AuthResponse;
import com.bankflow.api.dto.AuthDtos.LoginRequest;
import com.bankflow.api.dto.AuthDtos.LogoutRequest;
import com.bankflow.api.dto.AuthDtos.RefreshRequest;
import com.bankflow.api.dto.AuthDtos.RegisterRequest;
import com.bankflow.api.exception.DuplicateRequestException;
import com.bankflow.api.exception.ValidationException;
import com.bankflow.api.repository.RefreshTokenRepository;
import com.bankflow.api.repository.UserRepository;
import com.bankflow.api.security.BankFlowPrincipal;
import com.bankflow.api.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenDays;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        AuditService auditService,
        RefreshTokenRepository refreshTokenRepository,
        @Value("${bankflow.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateRequestException("Email is already registered");
        }
        User user = userRepository.save(new User(email, request.fullName().trim(), passwordEncoder.encode(request.password()), Set.of(Role.CUSTOMER)));
        BankFlowPrincipal principal = new BankFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRoles(), user.isEnabled());
        auditService.record(user.getId().toString(), AuditAction.USER_REGISTERED, "USER", user.getId().toString(), "Customer registered");
        return authResponse(user, principal);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow();
        BankFlowPrincipal principal = new BankFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRoles(), user.isEnabled());
        auditService.record(user.getId().toString(), AuditAction.LOGIN_SUCCESS, "USER", user.getId().toString(), "Login succeeded");
        return authResponse(user, principal);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
            .orElseThrow(() -> new ValidationException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        if (!token.isActive()) {
            throw new ValidationException("INVALID_REFRESH_TOKEN", "Refresh token is expired or revoked");
        }
        token.revoke();
        User user = token.getUser();
        BankFlowPrincipal principal = new BankFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRoles(), user.isEnabled());
        auditService.record(user.getId().toString(), AuditAction.REFRESH_TOKEN_ROTATED, "USER", user.getId().toString(), "Refresh token rotated");
        return authResponse(user, principal);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
            .filter(RefreshToken::isActive)
            .ifPresent(token -> {
                token.revoke();
                auditService.record(token.getUser().getId().toString(), AuditAction.LOGOUT, "USER", token.getUser().getId().toString(), "Refresh token revoked");
            });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        auditService.record(userId.toString(), AuditAction.LOGOUT_ALL, "USER", userId.toString(), "All refresh tokens revoked");
    }

    @Transactional
    public int purgeExpiredRefreshTokens() {
        return refreshTokenRepository.deleteExpiredBefore(Instant.now());
    }

    private AuthResponse authResponse(User user, BankFlowPrincipal principal) {
        String rawRefreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS)));
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRoles(), jwtService.issue(principal), rawRefreshToken);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
