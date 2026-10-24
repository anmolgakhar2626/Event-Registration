package com.example.eventRegisteration.repository;

import com.example.eventRegisteration.entity.EmailRetryEntry;
import com.example.eventRegisteration.entity.EmailRetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailRetryRepository extends JpaRepository<EmailRetryEntry, Long> {

    /** Fetch pending emails ready for retry, capped at 50 per batch */
    List<EmailRetryEntry> findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            EmailRetryStatus status, LocalDateTime now);
}
