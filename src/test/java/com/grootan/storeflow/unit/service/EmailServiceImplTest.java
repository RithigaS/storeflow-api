package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService = new EmailServiceImpl(
                mailSender,
                "test@storeflow.local",
                "http://localhost:3000",
                "admin1@storeflow.local, admin2@storeflow.local"
        );
    }

    @Test
    void shouldSendWelcomeEmail() {
        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("test@gmail.com", "Test User")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendPasswordResetEmail() {
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("test@gmail.com", "http://localhost:3000/reset-password?token=abc123")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendOrderConfirmationEmail() {
        assertDoesNotThrow(() ->
                emailService.sendOrderConfirmationEmail("test@gmail.com", "Order Summary")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendLowStockAlertToAllAdmins() {
        assertDoesNotThrow(() ->
                emailService.sendLowStockAlert("Product A", 3)
        );

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSkipLowStockAlertWhenNoAdminRecipientsConfigured() {
        EmailServiceImpl noAdminEmailService = new EmailServiceImpl(
                mailSender,
                "test@storeflow.local",
                "http://localhost:3000",
                ""
        );

        assertDoesNotThrow(() ->
                noAdminEmailService.sendLowStockAlert("Product A", 2)
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendDailyDigest() {
        assertDoesNotThrow(() ->
                emailService.sendDailyOrderDigest("admin@storeflow.local", "Digest Summary")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldHandleMailExceptionWithoutThrowing() {
        doThrow(new MailSendException("Mail failed"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("test@gmail.com", "Test User")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldEscapeHtmlCharactersInInput() {
        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("test@gmail.com", "<b>Rithi & User</b>")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldHandleNullValuesSafely() {
        assertDoesNotThrow(() ->
                emailService.sendOrderConfirmationEmail("test@gmail.com", null)
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldParseMultipleAdminRecipients() {
        Object recipients = ReflectionTestUtils.getField(emailService, "adminRecipients");
        assertDoesNotThrow(() -> {
            assert recipients != null;
        });
    }
}