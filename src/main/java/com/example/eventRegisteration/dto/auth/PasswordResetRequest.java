package com.example.eventRegisteration.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Email String email,
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String newPassword
) {}
