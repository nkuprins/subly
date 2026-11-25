package com.app.subly.service;

import com.app.subly.client.ResendAPIClient;
import com.app.subly.config.ReminderServiceConfig;
import com.app.subly.dto.reminder.ReminderDTO;
import com.app.subly.entity.reminder.SubscriptionReminder;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.EmailSendingPriority;
import com.app.subly.exception.client.*;
import com.app.subly.exception.reminder.ReminderAlreadyExistsError;
import com.app.subly.exception.reminder.ReminderUpdateError;
import com.app.subly.repository.SubscriptionReminderRepository;
import com.app.subly.util.EmailSendingPolicy;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionReminderService {
    private final SubscriptionReminderRepository repository;
    private final ResendAPIClient emailClient;
    private final EmailTemplateManager templateManager;
    private final ReminderServiceConfig config;

    private boolean isEmailApiCallingBlocked = false;
    private boolean permissionDenied = false;
    private int rateLimitRetryAttempts = 0;

    @Transactional(readOnly = true)
    public boolean isEnabledRemindersFor(Long userId) {
        return repository.existsByMembership_User_IdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public boolean isEnabledRemindersFor(String username) {
        return repository.existsByMembership_User_UsernameAndIsActiveTrue(username);
    }

    @Transactional
    public void addReminder(SubscriptionMembership membership, boolean isActive) {
        // Adds new reminder to the database with the specified status

        log.debug("Adding new reminder for membershipId={}, status={}", membership.getId(), isActive);
        try {
            SubscriptionReminder reminder = new SubscriptionReminder(membership, isActive);
            this.repository.save(reminder);
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to add new reminder for membershipId={}, status={}", membership.getId(), isActive);
            throw new ReminderAlreadyExistsError("Reminder for this membership already exists");
        }
    }

    @Transactional
    public void changeRemindersStatusForUser(Long userId, boolean isActive) {
        // Sets status for all the reminders for the current user.

        try {
            int changedCount = this.repository.changeRemindersStatusForUser(userId, isActive);
            log.info("Changed status for {} reminders, userId={}, isActive={}", changedCount, userId, isActive);
        } catch (DataAccessException ex) {
            log.error(
                    "Error occurred during changing reminder status (userId={}, isActive={}): {}",
                    userId,
                    isActive,
                    ex.getMessage());
            throw new ReminderUpdateError("Cannot change status of user's reminders");
        }
    }

    @Transactional
    public void changeReminderStatusForMembership(Long membershipId, boolean isActive) {
        // Sets a new status for reminder found by membership id
        int updatedRows = this.repository.changeReminderStatusByMembershipId(membershipId, isActive);

        if (updatedRows != 1) {
            throw new ReminderUpdateError("Cannot change status for the current membership");
        }
    }

    @Transactional
    public void runEmailSending() {
        // general function that runs "email sending loop"
        // it combines email filters and sorting, client API calls,
        // and makes decisions in special cases.
        // This function is intended to be called using @Scheduled annotation.

        if (this.isEmailApiCallingBlocked) {
            log.warn("Reminder service is blocked. Update email API Key and reload the application");
            return;
        }

        Map<String, List<ReminderDTO>> reminders = this.getGroupedRemindersByEmail();

        for (String email : reminders.keySet()) {
            if (reminders.get(email).isEmpty()) {
                continue;
            }

            Map<EmailSendingPriority, List<ReminderDTO>> remindersByPriority =
                    this.sortRemindersByPriority(reminders.get(email));

            if (remindersByPriority.isEmpty()) {
                continue;
            }

            String emailBody = templateManager.buildHtmlBody(remindersByPriority);

            rateLimitRetryAttempts = 0;
            boolean emailSentSuccessfully = this.processEmail(email, emailBody);

            if (this.isEmailApiCallingBlocked) {
                break;
            }

            if (this.permissionDenied) {
                this.permissionDenied = false;
                break;
            }

            if (emailSentSuccessfully) {
                this.updateSentAtForReminders(remindersByPriority);
            }

            makeDelay(config.getPostDelayMs());
        }
    }

    public void updateSentAtForReminders(Map<EmailSendingPriority, List<ReminderDTO>> remindersByPriority) {
        LocalDate sentAt = LocalDate.now();
        remindersByPriority.forEach((priority, priorityReminders) -> priorityReminders.forEach(reminder -> {
            int stage = mapSentStageFromPriority(priority);
            if (stage == -1) {
                log.warn(
                        "Cannot determine sent stage for reminderId={}, priority={}",
                        reminder.getReminderId(),
                        priority);
                return;
            }

            int updatedRows = repository.updateSentAt(sentAt, reminder.getReminderId(), stage);
            if (updatedRows != 1) {
                log.warn(
                        "Could not update sent date for reminderId={}, stage={}, rowsUpdated={}",
                        reminder.getReminderId(),
                        stage,
                        updatedRows);
            }
        }));
    }

    private int mapSentStageFromPriority(EmailSendingPriority priority) {
        return switch (priority) {
            case ONE_DAY, FREE_TRIAL_ONE_DAY -> 1;
            case THREE_DAYS, FREE_TRIAL_THREE_DAYS -> 2;
            default -> -1;
        };
    }

    private Map<String, List<ReminderDTO>> getGroupedRemindersByEmail() {
        // groups all reminders by email.
        // returns Map with the following structure: email -> [reminder, reminder2, ...]

        return this.repository.getAllActiveReminders().stream().collect(Collectors.groupingBy(ReminderDTO::getEmail));
    }

    private EnumMap<EmailSendingPriority, List<ReminderDTO>> sortRemindersByPriority(List<ReminderDTO> reminders) {
        EnumMap<EmailSendingPriority, List<ReminderDTO>> emailsByPriority = new EnumMap<>(EmailSendingPriority.class);
        for (ReminderDTO reminder : reminders) {
            EmailSendingPriority priority = EmailSendingPolicy.defineReminderSendingPriority(reminder);

            if (priority.equals(EmailSendingPriority.NONE)) {
                continue;
            }

            log.debug("Subscription: paymentData={}, status: {}", reminder.getSubscriptionNextPaymentDate(), priority);
            if (emailsByPriority.containsKey(priority)) {
                emailsByPriority.get(priority).add(reminder);
                continue;
            }

            List<ReminderDTO> remindersList = new ArrayList<>();
            remindersList.add(reminder);
            emailsByPriority.put(priority, remindersList);
        }
        log.debug("Total sorted result: {}", emailsByPriority);
        return emailsByPriority;
    }

    private void makeDelay(Long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while thread was sleeping");
        }
    }

    private boolean processEmail(String receiverEmail, String emailBody) {
        while (true) {
            try {
                this.emailClient.sendEmail(
                        this.config.getDefaultEmail(), receiverEmail, "Subscriptions end!", emailBody);
                rateLimitRetryAttempts = 0;
                return true;
            } catch (RateLimitExceededException ex) {
                // retry case
                if (rateLimitRetryAttempts >= config.getRetryAttempts()) {
                    log.error(
                            "Rate limit retry limit reached for {} after {} retries",
                            receiverEmail,
                            rateLimitRetryAttempts);
                    return false;
                }

                rateLimitRetryAttempts++;
                log.warn(
                        "Rate limit exceeded for {}. Retry {}/{} after {} ms",
                        receiverEmail,
                        rateLimitRetryAttempts,
                        config.getRetryAttempts(),
                        config.getRetryDelayMs());

                makeDelay(config.getRetryDelayMs());
            } catch (InvalidApiKeyException ex) {
                log.error(
                        "Cannot call email api: {}. The service will be blocked. Update your API Key and try again",
                        ex.getMessage());
                this.isEmailApiCallingBlocked = true;
                return false;
            } catch (ApiKeyAccessException ex) {
                log.error("Cannot send email: {}. Update your permissions", ex.getMessage());
                this.permissionDenied = true;
                return false;
            } catch (EmailClientException | APIServerException ex) {
                log.error(ex.getMessage());
                return false;
            }
        }
    }
}
