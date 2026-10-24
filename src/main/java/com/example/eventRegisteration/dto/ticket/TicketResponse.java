package com.example.eventRegisteration.dto.ticket;

import com.example.eventRegisteration.entity.Ticket;
import com.example.eventRegisteration.entity.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String token,
        TicketStatus status,
        String eventTitle,
        String venue,
        LocalDateTime eventDate,
        String attendeeName,
        LocalDateTime issuedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getToken(),
                ticket.getStatus(),
                ticket.getRegistration().getEvent().getTitle(),
                ticket.getRegistration().getEvent().getVenue(),
                ticket.getRegistration().getEvent().getEventDate(),
                ticket.getRegistration().getUser().getName(),
                ticket.getIssuedAt()
        );
    }
}
