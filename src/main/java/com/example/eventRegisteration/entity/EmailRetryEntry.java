package com.example.eventRegisteration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_retry_queue")
@Getter @Setter @NoArgsConstructor
public class EmailRetryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailRetryStatus status = EmailRetryStatus.PENDING;

    public static EmailRetryEntry of(String recipient, String subject, String body) {
        EmailRetryEntry e = new EmailRetryEntry();
        e.recipient = recipient;
        e.subject = subject;
        e.body = body;
        return e;
    }
}
