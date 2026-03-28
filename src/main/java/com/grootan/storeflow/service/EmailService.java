package com.grootan.storeflow.service;

public interface EmailService {


    void sendPasswordResetEmail(String to, String resetLink);

    void sendWelcomeEmail(String to, String fullName);

    void sendOrderConfirmationEmail(String to, String orderSummary);

    void sendLowStockAlert(String productName, int stock);

    void sendDailyOrderDigest(String to, String summary);
}