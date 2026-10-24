package com.example.eventRegisteration.controller;

import com.example.eventRegisteration.dto.admin.EventStatsResponse;
import com.example.eventRegisteration.dto.event.EventResponse;
import com.example.eventRegisteration.service.AdminService;
import com.example.eventRegisteration.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints.
 *
 * GET    /api/admin/events                   — all events (including drafts)
 * GET    /api/admin/events/{id}/stats        — registration analytics for an event
 * PATCH  /api/admin/events/{id}/cancel       — force cancel any event
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final EventService eventService;

    public AdminController(AdminService adminService, EventService eventService) {
        this.adminService = adminService;
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/events/{id}/stats")
    public ResponseEntity<EventStatsResponse> getEventStats(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEventStats(id));
    }

    @PatchMapping("/events/{id}/cancel")
    public ResponseEntity<EventResponse> forceCancel(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.adminCancelEvent(id));
    }
}
