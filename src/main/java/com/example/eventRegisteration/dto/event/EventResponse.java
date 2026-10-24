package com.example.eventRegisteration.dto.event;

import com.example.eventRegisteration.entity.Event;
import com.example.eventRegisteration.entity.EventStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        Long organiserId,
        String organiserName,
        String title,
        String description,
        String venue,
        LocalDateTime eventDate,
        int maxCapacity,
        int waitlistCap,
        BigDecimal price,
        EventStatus status,
        boolean featured,
        boolean refundEligible,
        LocalDateTime createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOrganiser().getId(),
                event.getOrganiser().getName(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getMaxCapacity(),
                event.getWaitlistCap(),
                event.getPrice(),
                event.getStatus(),
                event.isFeatured(),
                event.isRefundEligible(),
                event.getCreatedAt()
        );
    }
}
