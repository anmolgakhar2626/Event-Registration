package com.example.eventRegisteration.service;

import com.example.eventRegisteration.entity.EmailRetryEntry;
import com.example.eventRegisteration.repository.EmailRetryRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Async email service with retry queue.
 * Failed sends are logged to email_retry_queue and re-processed by a scheduled job.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final JavaMailSender mailSender;
    private final EmailRetryRepository retryRepository;

    public EmailService(JavaMailSender mailSender, EmailRetryRepository retryRepository) {
        this.mailSender = mailSender;
        this.retryRepository = retryRepository;
    }

    @Async("emailExecutor")
    public void sendEmailVerification(String to, String name, String token) {
        String subject = "Verify your email – EventHub";
        String body = "<p>Hi " + name + ",</p>"
                + "<p>Click below to verify your email:</p>"
                + "<p><a href='http://localhost:8080/api/auth/verify-email?token=" + token + "'>Verify Email</a></p>"
                + "<p>This link expires in 15 minutes.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendPasswordReset(String to, String name, String token) {
        String subject = "Password Reset – EventHub";
        String body = "<p>Hi " + name + ",</p>"
                + "<p>Click below to reset your password:</p>"
                + "<p><a href='http://localhost:8080/api/auth/reset-password?token=" + token + "'>Reset Password</a></p>"
                + "<p>This link expires in 30 minutes.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendRegistrationConfirmed(String to, String name, String eventTitle) {
        String subject = "Registration Confirmed – " + eventTitle;
        String body = "<p>Hi " + name + ",</p>"
                + "<p>Your registration for <strong>" + eventTitle + "</strong> has been confirmed!</p>"
                + "<p>Your ticket QR code is available in the app.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendWaitlistJoined(String to, String name, String eventTitle) {
        String subject = "Waitlisted – " + eventTitle;
        String body = "<p>Hi " + name + ",</p>"
                + "<p>You have been added to the waitlist for <strong>" + eventTitle + "</strong>.</p>"
                + "<p>We will notify you if a spot opens up.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendWaitlistPromoted(String to, String name, String eventTitle) {
        String subject = "Great news – You're in! " + eventTitle;
        String body = "<p>Hi " + name + ",</p>"
                + "<p>A spot opened up! Your registration for <strong>" + eventTitle + "</strong> is now confirmed.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendEventCancelled(String to, String name, String eventTitle) {
        String subject = "Event Cancelled – " + eventTitle;
        String body = "<p>Hi " + name + ",</p>"
                + "<p>We regret to inform you that <strong>" + eventTitle + "</strong> has been cancelled.</p>"
                + "<p>If you paid, a refund will be processed shortly.</p>";
        sendOrQueue(to, subject, body);
    }

    @Async("emailExecutor")
    public void sendEventReminder(String to, String name, String eventTitle, LocalDateTime eventDate) {
        String subject = "Reminder: " + eventTitle + " is tomorrow!";
        String body = "<p>Hi " + name + ",</p>"
                + "<p>This is a reminder that <strong>" + eventTitle + "</strong> is happening on "
                + eventDate + ".</p>";
        sendOrQueue(to, subject, body);
    }

    // ── Retry processor (runs every 5 minutes) ──────────────────────────
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void processRetryQueue() {
        List<EmailRetryEntry> pending = retryRepository
                .findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                        com.example.eventRegisteration.entity.EmailRetryStatus.PENDING,
                        LocalDateTime.now());

        for (EmailRetryEntry entry : pending) {
            try {
                doSend(entry.getRecipient(), entry.getSubject(), entry.getBody());
                entry.setStatus(com.example.eventRegisteration.entity.EmailRetryStatus.SENT);
            } catch (Exception ex) {
                entry.setAttempts(entry.getAttempts() + 1);
                if (entry.getAttempts() >= MAX_ATTEMPTS) {
                    entry.setStatus(com.example.eventRegisteration.entity.EmailRetryStatus.FAILED);
                } else {
                    // Exponential backoff: 5m, 25m, ...
                    entry.setNextRetryAt(LocalDateTime.now().plusMinutes(5L * entry.getAttempts()));
                }
            }
            retryRepository.save(entry);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────
    private void sendOrQueue(String to, String subject, String body) {
        try {
            doSend(to, subject, body);
        } catch (Exception ex) {
            log.warn("Email send failed, queueing for retry. to={} subject={} error={}", to, subject, ex.getMessage());
            retryRepository.save(EmailRetryEntry.of(to, subject, body));
        }
    }

    private void doSend(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }
}
