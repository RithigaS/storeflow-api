package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EmailServiceImplTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Mock JavaMailSender
        mailSender = Mockito.mock(JavaMailSender.class);

        // FIX: mock MimeMessage properly
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Initialize service
        emailService = new EmailServiceImpl(
                mailSender,
                "test@storeflow.local",
                "http://localhost:3000",
                "admin@storeflow.local"
        );
    }

    @Test
    void shouldSendWelcomeEmail() {
        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("test@gmail.com", "Test User")
        );
    }

    @Test
    void shouldSendPasswordResetEmail() {
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("test@gmail.com", "http://reset-link")
        );
    }

    @Test
    void shouldSendOrderConfirmationEmail() {
        assertDoesNotThrow(() ->
                emailService.sendOrderConfirmationEmail("test@gmail.com", "Order Summary")
        );
    }

    @Test
    void shouldSendLowStockAlert() {
        assertDoesNotThrow(() ->
                emailService.sendLowStockAlert("Product", 3)
        );
    }

    @Test
    void shouldSendDailyDigest() {
        assertDoesNotThrow(() ->
                emailService.sendDailyOrderDigest("admin@storeflow.local", "Summary")
        );
    }
}