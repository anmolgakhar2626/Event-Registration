package com.example.eventRegisteration.controller;

import com.example.eventRegisteration.dto.auth.LoginRequest;
import com.example.eventRegisteration.dto.auth.PasswordResetRequest;
import com.example.eventRegisteration.dto.auth.RegisterRequest;
import com.example.eventRegisteration.dto.auth.RegisterResponse;
import com.example.eventRegisteration.dto.auth.TokenResponse;
import com.example.eventRegisteration.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth endpoints — all public (see SecurityConfig).
 *
 * POST /api/auth/register          — register, triggers email verification
 * GET  /api/auth/verify-email      — verify token from email link
 * POST /api/auth/login             — login, returns JWT + refresh
 * POST /api/auth/refresh           — exchange refresh token for new pair
 * POST /api/auth/forgot-password   — initiate password reset
 * POST /api/auth/reset-password    — complete password reset
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent if account exists"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
