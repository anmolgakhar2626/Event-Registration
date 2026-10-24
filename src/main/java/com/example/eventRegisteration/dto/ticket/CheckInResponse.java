package com.example.eventRegisteration.dto.ticket;

import com.example.eventRegisteration.entity.Ticket;
import com.example.eventRegisteration.entity.TicketStatus;
import java.time.LocalDateTime;

public record CheckInResponse(
        Long ticketId,
        String token,
        TicketStatus status,
        String attendeeName,
        String eventTitle,
        LocalDateTime checkedInAt,
        boolean alreadyUsed
) {
    public static CheckInResponse from(Ticket ticket, boolean alreadyUsed) {
        return new CheckInResponse(
                ticket.getId(),
                ticket.getToken(),
                ticket.getStatus(),
                ticket.getRegistration().getUser().getName(),
                ticket.getRegistration().getEvent().getTitle(),
                ticket.getCheckedInAt(),
                alreadyUsed
        );
    }
}
