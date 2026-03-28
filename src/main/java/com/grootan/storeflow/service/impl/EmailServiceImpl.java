package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("Sending reset password email to {} with link {}", to, resetLink);
    }

    // Phase 8: Welcome Email
    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        log.info("Sending welcome email to {} for user {}", to, fullName);
    }

    // Phase 8: Order Confirmation
    @Override
    public void sendOrderConfirmationEmail(String to, String orderSummary) {
        log.info("Sending order confirmation email to {} with summary {}", to, orderSummary);
    }

    // Phase 8: Low Stock Alert
    @Override
    public void sendLowStockAlert(String productName, int stock) {
        log.warn("Low stock alert for product {}. Remaining stock {}", productName, stock);
    }

    // Phase 8: Daily Digest
    @Override
    public void sendDailyOrderDigest(String to, String summary) {
        log.info("Sending daily order digest to {} with summary {}", to, summary);
    }
}