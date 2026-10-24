package com.example.eventRegisteration.service;

import com.example.eventRegisteration.common.ResourceNotFoundException;
import com.example.eventRegisteration.dto.ticket.CheckInResponse;
import com.example.eventRegisteration.dto.ticket.TicketResponse;
import com.example.eventRegisteration.entity.Ticket;
import com.example.eventRegisteration.entity.TicketStatus;
import com.example.eventRegisteration.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ticketing Module.
 *
 * Idempotent check-in: scanning an already-USED ticket returns 200 (not error),
 * preventing double-scan issues at event entry.
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketByToken(String token) {
        Ticket ticket = findByToken(token);
        return TicketResponse.from(ticket);
    }

    /**
     * Idempotent check-in:
     * - CONFIRMED → mark USED, return 200
     * - USED      → return 200 (idempotent, not an error)
     * - CANCELLED → throw conflict
     */
    @Transactional
    public CheckInResponse checkIn(String token) {
        Ticket ticket = findByToken(token);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is cancelled and cannot be used for check-in");
        }

        boolean alreadyUsed = ticket.getStatus() == TicketStatus.USED;
        if (!alreadyUsed) {
            ticket.setStatus(TicketStatus.USED);
            ticket.setCheckedInAt(LocalDateTime.now());
            ticketRepository.save(ticket);
        }

        return CheckInResponse.from(ticket, alreadyUsed);
    }

    private Ticket findByToken(String token) {
        return ticketRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + token));
    }
}
