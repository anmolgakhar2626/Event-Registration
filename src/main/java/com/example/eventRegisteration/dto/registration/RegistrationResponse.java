package com.example.eventRegisteration.dto.registration;

import com.example.eventRegisteration.entity.Registration;
import com.example.eventRegisteration.entity.RegistrationStatus;
import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long userId,
        Long eventId,
        String eventTitle,
        RegistrationStatus status,
        LocalDateTime registeredAt,
        String ticketToken
) {
    public static RegistrationResponse from(Registration reg, String ticketToken) {
        return new RegistrationResponse(
                reg.getId(),
                reg.getUser().getId(),
                reg.getEvent().getId(),
                reg.getEvent().getTitle(),
                reg.getStatus(),
                reg.getRegisteredAt(),
                ticketToken
        );
    }
}
