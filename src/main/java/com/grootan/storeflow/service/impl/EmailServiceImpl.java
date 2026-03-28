package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String frontendBaseUrl;
    private final List<String> adminRecipients;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromEmail,
            @Value("${app.mail.frontend-base-url}") String frontendBaseUrl,
            @Value("${app.mail.admin-recipients:}") String adminRecipients
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.frontendBaseUrl = frontendBaseUrl;
        this.adminRecipients = parseRecipients(adminRecipients);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Reset Your StoreFlow Password";
        String htmlBody = """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #222;">
                        <h2 style="color: #2563eb;">Password Reset Request</h2>
                        <p>We received a request to reset your StoreFlow password.</p>
                        <p>
                            <a href="%s"
                               style="display: inline-block; padding: 10px 16px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 6px;">
                                Reset Password
                            </a>
                        </p>
                        <p>This link is time-limited. If you did not request this, you can safely ignore this email.</p>
                    </body>
                </html>
                """.formatted(resetLink);

        sendHtmlEmail(to, subject, htmlBody);
    }

    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        String verificationLink = frontendBaseUrl + "/verify-email";
        String subject = "Welcome to StoreFlow";
        String htmlBody = """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #222;">
                        <h2 style="color: #16a34a;">Welcome to StoreFlow</h2>
                        <p>Hi %s,</p>
                        <p>Your account has been created successfully.</p>
                        <p>Please verify your email to complete setup.</p>
                        <p>
                            <a href="%s"
                               style="display: inline-block; padding: 10px 16px; background-color: #16a34a; color: white; text-decoration: none; border-radius: 6px;">
                                Verify Email
                            </a>
                        </p>
                        <p>We're happy to have you with us.</p>
                    </body>
                </html>
                """.formatted(escape(fullName), verificationLink);

        sendHtmlEmail(to, subject, htmlBody);
    }

    @Override
    public void sendOrderConfirmationEmail(String to, String orderSummary) {
        String subject = "Your StoreFlow Order is Confirmed";
        String htmlBody = """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #222;">
                        <h2 style="color: #7c3aed;">Order Confirmation</h2>
                        <p>Your order has been confirmed successfully.</p>
                        <p>Here is your order summary:</p>
                        <div style="padding: 12px; border: 1px solid #ddd; border-radius: 6px; background-color: #f8fafc; white-space: pre-wrap;">%s</div>
                        <p>Thank you for shopping with StoreFlow.</p>
                    </body>
                </html>
                """.formatted(escape(orderSummary));

        sendHtmlEmail(to, subject, htmlBody);
    }

    @Override
    public void sendLowStockAlert(String productName, int stock) {
        if (adminRecipients.isEmpty()) {
            log.warn("Skipping low-stock alert because no admin recipients are configured");
            return;
        }

        String subject = "StoreFlow Low Stock Alert";
        String htmlBody = """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #222;">
                        <h2 style="color: #dc2626;">Low Stock Alert</h2>
                        <p>A product has dropped below the configured threshold.</p>
                        <table style="border-collapse: collapse; margin-top: 12px;">
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><strong>Product</strong></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><strong>Remaining Stock</strong></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">%d</td>
                            </tr>
                        </table>
                    </body>
                </html>
                """.formatted(escape(productName), stock);

        for (String adminRecipient : adminRecipients) {
            sendHtmlEmail(adminRecipient, subject, htmlBody);
        }
    }

    @Override
    public void sendDailyOrderDigest(String to, String summary) {
        String subject = "StoreFlow Daily Order Digest";
        String htmlBody = """
                <html>
                    <body style="font-family: Arial, sans-serif; color: #222;">
                        <h2 style="color: #0f766e;">Daily Order Digest</h2>
                        <p>Here is your daily order summary:</p>
                        <div style="padding: 12px; border: 1px solid #ddd; border-radius: 6px; background-color: #f8fafc; white-space: pre-wrap;">%s</div>
                    </body>
                </html>
                """.formatted(escape(summary));

        sendHtmlEmail(to, subject, htmlBody);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to {} with subject {}", to, subject);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send email to {} with subject {}. Continuing without breaking flow.", to, subject, ex);
        }
    }

    private List<String> parseRecipients(String recipients) {
        if (recipients == null || recipients.isBlank()) {
            return List.of();
        }

        return Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}