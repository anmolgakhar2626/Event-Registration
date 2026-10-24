package com.example.eventRegisteration.dto.auth;

import com.example.eventRegisteration.entity.Role;
import com.example.eventRegisteration.entity.User;
import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean verified,
        LocalDateTime createdAt
) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }
}
