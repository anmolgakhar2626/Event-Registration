package com.example.eventRegisteration.service;

import com.example.eventRegisteration.common.ResourceNotFoundException;
import com.example.eventRegisteration.dto.admin.EventStatsResponse;
import com.example.eventRegisteration.entity.Event;
import com.example.eventRegisteration.entity.RegistrationStatus;
import com.example.eventRegisteration.entity.TicketStatus;
import com.example.eventRegisteration.repository.EventRepository;
import com.example.eventRegisteration.repository.RegistrationRepository;
import com.example.eventRegisteration.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final TicketRepository ticketRepository;

    public AdminService(EventRepository eventRepository,
                        RegistrationRepository registrationRepository,
                        TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.ticketRepository = ticketRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public EventStatsResponse getEventStats(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        long confirmed  = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        long waitlisted = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        long cancelled  = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
        long checkedIn  = ticketRepository.countByRegistrationEventIdAndStatus(eventId, TicketStatus.USED);

        BigDecimal revenue = event.getPrice().multiply(BigDecimal.valueOf(confirmed));

        return new EventStatsResponse(
                eventId,
                event.getTitle(),
                confirmed + waitlisted + cancelled,
                confirmed,
                waitlisted,
                cancelled,
                checkedIn,
                revenue
        );
    }
}
