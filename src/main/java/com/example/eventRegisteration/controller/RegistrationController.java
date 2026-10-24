package com.example.eventRegisteration.controller;

import com.example.eventRegisteration.dto.registration.RegistrationResponse;
import com.example.eventRegisteration.dto.registration.TeamRegisterRequest;
import com.example.eventRegisteration.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Registration endpoints.
 *
 * POST   /api/events/{id}/register            — ATTENDEE: register for event
 * POST   /api/events/{id}/register/team       — ATTENDEE: team registration
 * DELETE /api/registrations/{id}              — ATTENDEE: cancel registration (promotes waitlist)
 * GET    /api/registrations/mine              — ATTENDEE: my registrations
 */
@RestController
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping({"/api/events/{eventId}/register", "/api/registrations/{eventId}"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long eventId,
                                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(userId, eventId));
    }

    @PostMapping({"/api/events/{eventId}/register/team", "/api/registrations/{eventId}/team"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegistrationResponse>> registerTeam(@PathVariable Long eventId,
                                                                    @Valid @RequestBody TeamRegisterRequest req,
                                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerTeam(userId, eventId, req));
    }

    @DeleteMapping("/api/registrations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable Long id,
                                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        registrationService.cancelRegistration(id, userId);
        return ResponseEntity.ok(Map.of("message", "Registration cancelled"));
    }

    @GetMapping({"/api/registrations/mine", "/api/registrations/my"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegistrationResponse>> myRegistrations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(registrationService.getMyRegistrations(userId));
    }
}
