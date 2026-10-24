package com.example.eventRegisteration.service;

import com.example.eventRegisteration.common.ResourceNotFoundException;
import com.example.eventRegisteration.dto.registration.RegistrationResponse;
import com.example.eventRegisteration.dto.registration.TeamRegisterRequest;
import com.example.eventRegisteration.entity.*;
import com.example.eventRegisteration.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Registration & Capacity Module.
 *
 * Key engineering decisions:
 * - SELECT FOR UPDATE (findByIdForUpdate) prevents overselling under concurrent load.
 * - Waitlist promotion is synchronous within the cancellation transaction — exactly one promotion per cancel.
 * - Duplicate guard before insert to avoid hitting the UK constraint.
 */
@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final EmailService emailService;

    public RegistrationService(RegistrationRepository registrationRepository,
                                EventRepository eventRepository,
                                UserRepository userRepository,
                                TicketRepository ticketRepository,
                                TeamRegistrationRepository teamRegistrationRepository,
                                EmailService emailService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.emailService = emailService;
    }

    // ── Individual registration ───────────────────────────────────────────
    @Transactional
    public RegistrationResponse register(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Pessimistic write lock — SELECT ... FOR UPDATE
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Event is not open for registration");
        }

        // Duplicate guard
        if (registrationRepository.existsByUserIdAndEventIdAndStatusNot(userId, eventId, RegistrationStatus.CANCELLED)) {
            throw new IllegalStateException("You are already registered or waitlisted for this event");
        }

        long confirmed  = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        long waitlisted = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);

        Registration reg = new Registration();
        reg.setUser(user);
        reg.setEvent(event);

        String ticketToken = null;
        if (confirmed < event.getMaxCapacity()) {
            reg.setStatus(RegistrationStatus.CONFIRMED);
            registrationRepository.save(reg);
            ticketToken = issueTicket(reg);
            emailService.sendRegistrationConfirmed(user.getEmail(), user.getName(), event.getTitle());
        } else if (event.getWaitlistCap() > 0 && waitlisted < event.getWaitlistCap()) {
            reg.setStatus(RegistrationStatus.WAITLISTED);
            registrationRepository.save(reg);
            emailService.sendWaitlistJoined(user.getEmail(), user.getName(), event.getTitle());
        } else {
            throw new IllegalStateException("Event is full and waitlist is at capacity");
        }

        return RegistrationResponse.from(reg, ticketToken);
    }

    // ── Team registration ─────────────────────────────────────────────────
    @Transactional
    public List<RegistrationResponse> registerTeam(Long leadUserId, Long eventId, TeamRegisterRequest req) {
        User leadUser = userRepository.findById(leadUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead user not found"));

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Event is not open for registration");
        }

        // Resolve member IDs from emails
        List<String> memberEmails = req.memberEmails();
        List<User> members = new ArrayList<>();
        for (String email : memberEmails) {
            members.add(userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email)));
        }

        int teamSize = 1 + members.size();
        long confirmed = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        if (confirmed + teamSize > event.getMaxCapacity()) {
            throw new IllegalStateException("Not enough seats for the full team");
        }

        TeamRegistration team = new TeamRegistration();
        team.setLeadUser(leadUser);
        team.setEvent(event);
        team.setMemberCount(teamSize);
        team.setGroupToken(UUID.randomUUID().toString());
        teamRegistrationRepository.save(team);

        List<User> allMembers = new ArrayList<>();
        allMembers.add(leadUser);
        allMembers.addAll(members);

        List<RegistrationResponse> responses = new ArrayList<>();
        for (User member : allMembers) {
            if (registrationRepository.existsByUserIdAndEventIdAndStatusNot(
                    member.getId(), eventId, RegistrationStatus.CANCELLED)) {
                throw new IllegalStateException("User " + member.getEmail() + " is already registered for this event");
            }

            Registration reg = new Registration();
            reg.setUser(member);
            reg.setEvent(event);
            reg.setStatus(RegistrationStatus.CONFIRMED);
            reg.setTeamRegistration(team);
            registrationRepository.save(reg);
            String ticketToken = issueTicket(reg);
            emailService.sendRegistrationConfirmed(member.getEmail(), member.getName(), event.getTitle());
            responses.add(RegistrationResponse.from(reg, ticketToken));
        }

        return responses;
    }

    // ── Cancel ────────────────────────────────────────────────────────────
    @Transactional
    public void cancelRegistration(Long registrationId, Long requesterId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found: " + registrationId));

        if (!reg.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not own this registration");
        }
        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw new IllegalStateException("Registration is already cancelled");
        }

        boolean wasConfirmed = reg.getStatus() == RegistrationStatus.CONFIRMED;
        reg.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);

        ticketRepository.findByRegistrationId(registrationId).ifPresent(t -> {
            t.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(t);
        });

        // Synchronous waitlist promotion — inside same transaction
        if (wasConfirmed) {
            promoteNextWaitlisted(reg.getEvent().getId());
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getMyRegistrations(Long userId) {
        return registrationRepository.findByUserId(userId).stream()
                .map(r -> {
                    String token = ticketRepository.findByRegistrationId(r.getId())
                            .map(t -> t.getToken()).orElse(null);
                    return RegistrationResponse.from(r, token);
                })
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────
    private void promoteNextWaitlisted(Long eventId) {
        registrationRepository
                .findFirstByEventIdAndStatusOrderByRegisteredAtAsc(eventId, RegistrationStatus.WAITLISTED)
                .ifPresent(next -> {
                    next.setStatus(RegistrationStatus.CONFIRMED);
                    registrationRepository.save(next);
                    String token = issueTicket(next);
                    emailService.sendWaitlistPromoted(
                            next.getUser().getEmail(),
                            next.getUser().getName(),
                            next.getEvent().getTitle());
                });
    }

    private String issueTicket(Registration reg) {
        Ticket ticket = new Ticket();
        ticket.setRegistration(reg);
        ticket.setToken(UUID.randomUUID().toString());
        ticket.setStatus(TicketStatus.CONFIRMED);
        return ticketRepository.save(ticket).getToken();
    }
}
