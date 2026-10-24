package com.example.eventRegisteration.service;

import com.example.eventRegisteration.common.ResourceNotFoundException;
import com.example.eventRegisteration.dto.auth.LoginRequest;
import com.example.eventRegisteration.dto.auth.RegisterRequest;
import com.example.eventRegisteration.dto.auth.RegisterResponse;
import com.example.eventRegisteration.dto.auth.TokenResponse;
import com.example.eventRegisteration.entity.Role;
import com.example.eventRegisteration.entity.User;
import com.example.eventRegisteration.repository.UserRepository;
import com.example.eventRegisteration.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.email-verify.ttl-minutes:15}")
    private long emailVerifyTtlMinutes;

    @Value("${app.password-reset.ttl-minutes:30}")
    private long passwordResetTtlMinutes;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       StringRedisTemplate redisTemplate,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalStateException("Email already in use: " + req.email());
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.ATTENDEE);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "email-verify:" + token,
                String.valueOf(user.getId()),
                emailVerifyTtlMinutes,
                TimeUnit.MINUTES);
        emailService.sendEmailVerification(user.getEmail(), user.getName(), token);

        return RegisterResponse.from(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        String key = "email-verify:" + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            throw new IllegalStateException("Verification token expired or invalid");
        }
        User user = userRepository.findById(Long.parseLong(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isVerified()) {
            throw new IllegalStateException("Email not verified. Please check your inbox.");
        }

        String access  = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refresh,
                7,
                TimeUnit.DAYS);

        return TokenResponse.of(access, refresh, jwtProvider.getAccessTokenExpiryMs());
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new IllegalStateException("Invalid refresh token");
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!refreshToken.equals(stored)) {
            throw new IllegalStateException("Refresh token reuse detected or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccess  = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefresh = jwtProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        redisTemplate.opsForValue().set("refresh:" + userId, newRefresh, 7, TimeUnit.DAYS);

        return TokenResponse.of(newAccess, newRefresh, jwtProvider.getAccessTokenExpiryMs());
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "pwd-reset:" + token,
                String.valueOf(user.getId()),
                passwordResetTtlMinutes,
                TimeUnit.MINUTES);

        emailService.sendPasswordReset(user.getEmail(), user.getName(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = "pwd-reset:" + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            throw new IllegalStateException("Password reset token expired or invalid");
        }
        User user = userRepository.findById(Long.parseLong(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(key);
    }
}
