package com.example.eventRegisteration.service;

import com.example.eventRegisteration.entity.Event;
import com.example.eventRegisteration.entity.Registration;
import com.example.eventRegisteration.entity.RegistrationStatus;
import com.example.eventRegisteration.repository.EventRepository;
import com.example.eventRegisteration.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that sends 24-hour reminder emails to confirmed attendees.
 * Runs every hour via cron — queries events starting in the next 24–25 hours
 * to avoid duplicate sends between runs.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    public ReminderScheduler(EventRepository eventRepository,
                              RegistrationRepository registrationRepository,
                              EmailService emailService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.emailService = emailService;
    }

    /**
     * Runs every hour at the top of the hour.
     * Sends reminders for events starting between 24 h and 25 h from now.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24);
        LocalDateTime to   = LocalDateTime.now().plusHours(25);

        List<Event> upcoming = eventRepository.findUpcomingBetween(from, to);
        log.info("Reminder scheduler: found {} upcoming events in window [{}, {}]", upcoming.size(), from, to);

        for (Event event : upcoming) {
            List<Registration> attendees = registrationRepository.findByEventIdAndStatus(
                    event.getId(), RegistrationStatus.CONFIRMED);

            for (Registration reg : attendees) {
                emailService.sendEventReminder(
                        reg.getUser().getEmail(),
                        reg.getUser().getName(),
                        event.getTitle(),
                        event.getEventDate());
            }
            log.info("Sent reminders for event '{}' to {} attendees", event.getTitle(), attendees.size());
        }
    }
}
