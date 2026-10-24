package com.example.eventRegisteration.dto.admin;

import java.math.BigDecimal;

public record EventStatsResponse(
        Long eventId,
        String eventTitle,
        long totalRegistrations,
        long confirmedCount,
        long waitlistedCount,
        long cancelledCount,
        long checkedInCount,
        BigDecimal totalRevenue
) {}
