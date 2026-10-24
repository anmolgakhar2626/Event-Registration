package com.example.eventRegisteration.repository;

import com.example.eventRegisteration.entity.Event;
import com.example.eventRegisteration.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * PESSIMISTIC_WRITE → SELECT … FOR UPDATE.
     * Used during registration to prevent overselling the last seat.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deleted = false")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    /** Non-deleted events visible to attendees */
    @Query("SELECT e FROM Event e WHERE e.deleted = false AND e.status = :status")
    List<Event> findAllByStatusAndNotDeleted(@Param("status") EventStatus status);

    /** Events by organiser */
    List<Event> findByOrganiserIdAndDeletedFalse(Long organiserId);

    /** All non-deleted events (admin) */
    @Query("SELECT e FROM Event e WHERE e.deleted = false")
    List<Event> findAllNotDeleted();

    /** Events starting in the next N hours (for reminder scheduling) */
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.deleted = false " +
           "AND e.eventDate BETWEEN :from AND :to")
    List<Event> findUpcomingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
