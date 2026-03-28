package com.grootan.storeflow.scheduler;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyOrderDigestScheduler {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Value("${app.mail.admin-recipients:}")
    private String adminRecipients;

    @Value("${app.digest.enabled:true}")
    private boolean digestEnabled;

    // Runs every day at 11:59 PM
    @Scheduled(cron = "0 59 23 * * *")
    public void sendDailyDigest() {

        if (!digestEnabled) {
            log.info("Daily digest is disabled");
            return;
        }

        LocalDate today = LocalDate.now();

        List<Order> orders = orderRepository.findAll();

        StringBuilder summary = new StringBuilder();
        summary.append("Daily Order Summary (").append(today).append(")\n\n");

        int count = 0;

        for (Order order : orders) {
            if (order.getCreatedAt() != null &&
                    order.getCreatedAt().toLocalDate().equals(today)) {

                summary.append("Order ID: ").append(order.getId()).append("\n");
                summary.append("Customer: ").append(order.getCustomer().getFullName()).append("\n");
                summary.append("Amount: ").append(order.getTotalAmount()).append("\n");
                summary.append("Status: ").append(order.getStatus()).append("\n\n");

                count++;
            }
        }

        summary.append("Total Orders Today: ").append(count);

        if (count == 0) {
            log.info("No orders today, skipping digest");
            return;
        }

        // Send to all admins
        if (adminRecipients != null && !adminRecipients.isBlank()) {
            for (String email : adminRecipients.split(",")) {
                emailService.sendDailyOrderDigest(email.trim(), summary.toString());
            }
        }

        log.info("Daily order digest sent successfully");
    }
}