package com.app.subly.service;

import com.app.subly.dto.reminder.ReminderDTO;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.EmailSendingPriority;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class EmailTemplateManager {
    private final SpringTemplateEngine templateEngine;

    public EmailTemplateManager(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildHtmlBody(Map<EmailSendingPriority, List<ReminderDTO>> remindersByPriority) {
        List<EmailSection> sections = remindersByPriority.entrySet().stream()
                .filter(entry -> entry.getKey() != EmailSendingPriority.NONE)
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> new EmailSection(priorityTitle(entry.getKey()),
                        entry.getValue().stream()
                        .map(this::toEmailSubscription)
                        .toList()))
                .toList();

        Context context = new Context();
        context.setVariable("sections", sections);
        return templateEngine.process("email/reminders", context);
    }

    private EmailSubscription toEmailSubscription(ReminderDTO reminder) {
        return new EmailSubscription(
                reminder.getSubscriptionProviderName(),
                reminder.getSubscriptionPrice(),
                reminder.getPaymentCurrency().getSymbol(),
                reminder.isFreeTrial(),
                reminder.getBillingCycle() == BillingCycle.YEARLY ? "yearly" : "monthly",
                reminder.getSubscriptionNextPaymentDate());
    }

    private String priorityTitle(EmailSendingPriority priority) {
        return switch (priority) {
            case FREE_TRIAL_ONE_DAY -> "Free trial ending tomorrow";
            case FREE_TRIAL_THREE_DAYS -> "Free trial ending in three days";
            case ONE_DAY -> "Ending tomorrow";
            case THREE_DAYS -> "Ending in three days";
            case NONE -> "";
        };
    }

    // Internal records used only here

    public record EmailSection(String title, List<EmailSubscription> subscriptions) {}

    public record EmailSubscription(
            String providerName,
            BigDecimal price,
            String currencySymbol,
            boolean freeTrial,
            String billingPeriod,
            LocalDate endDate) {}
}
