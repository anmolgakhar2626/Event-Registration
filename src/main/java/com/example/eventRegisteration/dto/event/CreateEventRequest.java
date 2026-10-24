package com.example.eventRegisteration.dto.event;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        String description,

        @NotBlank(message = "Venue is required")
        String venue,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime eventDate,

        @Min(value = 1, message = "Max capacity must be at least 1")
        int maxCapacity,

        @Min(value = 0)
        int waitlistCap,

        @DecimalMin(value = "0.00")
        BigDecimal price
) {}
