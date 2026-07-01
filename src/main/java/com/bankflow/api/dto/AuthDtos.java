package com.bankflow.api.dto;

import com.bankflow.api.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(UUID userId, String email, String fullName, Set<Role> roles, String accessToken, String refreshToken) {
    }
}
