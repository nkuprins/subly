package com.app.subly.repository;

import com.app.subly.dto.reminder.ReminderDTO;
import com.app.subly.entity.reminder.SubscriptionReminder;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubscriptionReminderRepository extends JpaRepository<SubscriptionReminder, Long> {

    boolean existsByMembership_User_IdAndIsActiveTrue(Long userId);

    boolean existsByMembership_User_UsernameAndIsActiveTrue(String username);

    @Query("""
                SELECT new com.app.subly.dto.reminder.ReminderDTO(
                        sr.id,
                        u.email,
                        sp.name,
                        s.freeTrial,
                        s.price,
                        s.currency,
                        s.billingCycle,
                        s.nextPaymentDate,
                        sr.firstSentAt,
                        sr.secondSentAt
                    )
                FROM SubscriptionReminder sr JOIN sr.membership sm
                    JOIN sm.user u
                    JOIN sm.subscription s
                    JOIN s.provider sp
                WHERE sr.isActive

            """)
    List<ReminderDTO> getAllActiveReminders();

    // UPDATE subly.subscription_reminder AS sr
    //    JOIN subly.subscription_membership AS sm
    //    ON sm.id = sr.membership_id
    //   SET sr.is_active = FALSE
    //   WHERE sm.user_id = 2;

    @Transactional
    @Modifying
    @Query("""
            UPDATE SubscriptionReminder sr
               SET sr.isActive = :isActive
               WHERE sr.membership.user.id = :userId
            """)
    int changeRemindersStatusForUser(@Param("userId") Long userId, @Param("isActive") boolean isActive);

    @Transactional
    @Modifying
    @Query("""
            UPDATE SubscriptionReminder sr
               SET sr.isActive = :isActive
               WHERE sr.membership.id = :membershipId
            """)
    int changeReminderStatusByMembershipId(
            @Param("membershipId") Long membershipId, @Param("isActive") boolean isActive);

    @Transactional
    @Modifying
    @Query("""
        UPDATE SubscriptionReminder sr
           SET sr.firstSentAt = CASE
                   WHEN :stage = 1 THEN :sentAt
                   ELSE sr.firstSentAt
               END,
               sr.secondSentAt = CASE
                   WHEN :stage = 2 THEN :sentAt
                   ELSE sr.secondSentAt
               END
         WHERE sr.id = :reminderId
           AND :stage IN (1, 2)
        """)
    int updateSentAt(
            @Param("sentAt") LocalDate sentAt, @Param("reminderId") Long reminderId, @Param("stage") int stage);
}
