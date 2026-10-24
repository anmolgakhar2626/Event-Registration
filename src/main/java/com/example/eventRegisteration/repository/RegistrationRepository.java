package com.example.eventRegisteration.repository;

import com.example.eventRegisteration.entity.Registration;
import com.example.eventRegisteration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /** Duplicate registration guard — checks at service level before hitting UK constraint */
    boolean existsByUserIdAndEventIdAndStatusNot(Long userId, Long eventId, RegistrationStatus excludeStatus);

    /** Count confirmed registrations for capacity check */
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    /** Find the earliest waitlisted registration for promotion (FIFO) */
    Optional<Registration> findFirstByEventIdAndStatusOrderByRegisteredAtAsc(Long eventId, RegistrationStatus status);

    /** All confirmed registrations for an event (for notifications) */
    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    /** User's registrations */
    List<Registration> findByUserId(Long userId);

    /** Count registrations per event for admin analytics */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = :status")
    long countByEventAndStatus(@Param("eventId") Long eventId, @Param("status") RegistrationStatus status);
}
