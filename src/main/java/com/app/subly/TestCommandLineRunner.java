package com.app.subly;

import com.app.subly.client.ResendAPIClient;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionReminderRepository;
import com.app.subly.service.SubscriptionReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TestCommandLineRunner implements CommandLineRunner {
    private final SubscriptionReminderRepository reminderRepository;
    private final ResendAPIClient client;
    private final SubscriptionMembershipRepository repository;
    private final SubscriptionReminderService reminderService;

    @Override
    public void run(String... args) throws Exception {
        //        List<ReminderDTO> dto = reminderRepository.getAllActiveReminders();
        //
        //        dto.forEach((reminder) -> {
        //            log.info("Got reminder: {}", reminder.toString());
        //        });

        //        reminderRepository.changeReminderStatusForUser(2L, true);

        //        client.sendEmail("onboarding@resend.dev", "jsj612623@gmail.com", "Congratulations!", "<strong>Hello,
        // Andris!</string>"
        //        );

        //        SubscriptionMembership membership = repository.findById(4L).orElseThrow();
        //        reminderService.changeReminderStatusForUser(2L, true);
        //        log.info(reminderService.getGroupedRemindersByEmail().toString());
        //        this.reminderService.runEmailSending();
    }
}
