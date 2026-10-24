package com.example.eventRegisteration.repository;

import com.example.eventRegisteration.entity.Ticket;
import com.example.eventRegisteration.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByToken(String token);

    Optional<Ticket> findByRegistrationId(Long registrationId);

    long countByRegistrationEventIdAndStatus(Long eventId, TicketStatus status);
}
