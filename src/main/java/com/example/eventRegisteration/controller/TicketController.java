package com.example.eventRegisteration.controller;

import com.example.eventRegisteration.dto.ticket.CheckInResponse;
import com.example.eventRegisteration.dto.ticket.TicketResponse;
import com.example.eventRegisteration.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Ticketing endpoints.
 *
 * GET  /api/tickets/{token}          — retrieve ticket metadata
 * POST /api/tickets/{token}/checkin  — organiser check-in (idempotent)
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{token}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String token) {
        return ResponseEntity.ok(ticketService.getTicketByToken(token));
    }

    /**
     * Check-in endpoint — idempotent (double scan returns 200, not error).
     * ORGANISER role required to prevent attendees from self-checking in.
     * Supports both path variable /{token}/checkin and query param /check-in?token=
     */
    @PostMapping("/{token}/checkin")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public ResponseEntity<CheckInResponse> checkIn(@PathVariable String token) {
        return ResponseEntity.ok(ticketService.checkIn(token));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public ResponseEntity<CheckInResponse> checkInQuery(@RequestParam String token) {
        return ResponseEntity.ok(ticketService.checkIn(token));
    }
}
