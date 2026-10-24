package com.example.eventRegisteration.controller;

import com.example.eventRegisteration.dto.event.CreateEventRequest;
import com.example.eventRegisteration.dto.event.EventResponse;
import com.example.eventRegisteration.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Event management endpoints.
 *
 * GET  /api/events                      — public: list published events
 * GET  /api/events/{id}                 — public: get event detail
 * POST /api/events                      — ORGANISER: create event (DRAFT)
 * PATCH /api/events/{id}/publish        — ORGANISER: publish event
 * PATCH /api/events/{id}/unpublish      — ORGANISER: unpublish event
 * PATCH /api/events/{id}/cancel         — ORGANISER: cancel event + notify attendees
 * DELETE /api/events/{id}              — ORGANISER: soft delete
 * GET  /api/events/mine                 — ORGANISER: my events
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ── Public ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<EventResponse>> listPublished() {
        return ResponseEntity.ok(eventService.getPublishedEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    // ── Organiser ─────────────────────────────────────────────────────────

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<List<EventResponse>> myEvents(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(eventService.getMyEvents(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest req,
                                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(userId, req));
    }

    @RequestMapping(value = "/{id}/publish", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(eventService.publishEvent(id, userId));
    }

    @RequestMapping(value = "/{id}/unpublish", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> unpublishEvent(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(eventService.unpublishEvent(id, userId));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(eventService.cancelEvent(id, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        eventService.deleteEvent(id, userId);
        return ResponseEntity.ok(Map.of("message", "Event deleted"));
    }
}
