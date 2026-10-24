package com.example.eventRegisteration.service;

import com.example.eventRegisteration.common.ResourceNotFoundException;
import com.example.eventRegisteration.dto.event.CreateEventRequest;
import com.example.eventRegisteration.dto.event.EventResponse;
import com.example.eventRegisteration.entity.Event;
import com.example.eventRegisteration.entity.EventStatus;
import com.example.eventRegisteration.entity.Registration;
import com.example.eventRegisteration.entity.RegistrationStatus;
import com.example.eventRegisteration.entity.User;
import com.example.eventRegisteration.repository.EventRepository;
import com.example.eventRegisteration.repository.RegistrationRepository;
import com.example.eventRegisteration.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        RegistrationRepository registrationRepository,
                        EmailService emailService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public EventResponse createEvent(Long organiserId, CreateEventRequest req) {
        User organiser = userRepository.findById(organiserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = new Event();
        event.setOrganiser(organiser);
        event.setTitle(req.title());
        event.setDescription(req.description());
        event.setVenue(req.venue());
        event.setEventDate(req.eventDate());
        event.setMaxCapacity(req.maxCapacity());
        event.setWaitlistCap(req.waitlistCap());
        event.setPrice(req.price() != null ? req.price() : BigDecimal.ZERO);

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse publishEvent(Long eventId, Long requesterId) {
        Event event = getOwnedEvent(eventId, requesterId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT events can be published");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse unpublishEvent(Long eventId, Long requesterId) {
        Event event = getOwnedEvent(eventId, requesterId);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED events can be unpublished");
        }
        event.setStatus(EventStatus.DRAFT);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId, Long requesterId) {
        Event event = getOwnedEvent(eventId, requesterId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        event.setRefundEligible(true);
        eventRepository.save(event);

        List<Registration> confirmed = registrationRepository.findByEventIdAndStatus(
                eventId, RegistrationStatus.CONFIRMED);
        confirmed.forEach(r -> emailService.sendEventCancelled(
                r.getUser().getEmail(), r.getUser().getName(), event.getTitle()));

        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse adminCancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        event.setStatus(EventStatus.CANCELLED);
        event.setRefundEligible(true);
        eventRepository.save(event);

        List<Registration> confirmed = registrationRepository.findByEventIdAndStatus(
                eventId, RegistrationStatus.CONFIRMED);
        confirmed.forEach(r -> emailService.sendEventCancelled(
                r.getUser().getEmail(), r.getUser().getName(), event.getTitle()));

        return EventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long requesterId) {
        Event event = getOwnedEvent(eventId, requesterId);
        event.setDeleted(true);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPublishedEvents() {
        return eventRepository.findAllByStatusAndNotDeleted(EventStatus.PUBLISHED)
                .stream().map(EventResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long organiserId) {
        return eventRepository.findByOrganiserIdAndDeletedFalse(organiserId)
                .stream().map(EventResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAllNotDeleted()
                .stream().map(EventResponse::from).collect(Collectors.toList());
    }

    private Event getOwnedEvent(Long eventId, Long requesterId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (!event.getOrganiser().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not own this event");
        }
        return event;
    }
}
