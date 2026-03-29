package com.grootan.storeflow.unit.scheduler;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.service.EmailService;
import com.grootan.storeflow.scheduler.DailyOrderDigestScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class DailyOrderDigestSchedulerTest {

    private OrderRepository orderRepository;
    private EmailService emailService;
    private DailyOrderDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        emailService = mock(EmailService.class);

        scheduler = new DailyOrderDigestScheduler(orderRepository, emailService);
    }

    @Test
    void sendDailyDigestShouldReturnWhenDigestDisabled() {
        ReflectionTestUtils.setField(scheduler, "digestEnabled", false);
        ReflectionTestUtils.setField(scheduler, "adminRecipients", "admin@test.com");

        scheduler.sendDailyDigest();

        verify(orderRepository, never()).findAll();
        verify(emailService, never()).sendDailyOrderDigest(anyString(), anyString());
    }

    @Test
    void sendDailyDigestShouldReturnWhenNoOrdersToday() {
        ReflectionTestUtils.setField(scheduler, "digestEnabled", true);
        ReflectionTestUtils.setField(scheduler, "adminRecipients", "admin@test.com");

        Order oldOrder = buildOrder(1L, LocalDateTime.now().minusDays(1), "user@test.com", "User One");

        when(orderRepository.findAll()).thenReturn(List.of(oldOrder));

        scheduler.sendDailyDigest();

        verify(orderRepository, times(1)).findAll();
        verify(emailService, never()).sendDailyOrderDigest(anyString(), anyString());
    }

    @Test
    void sendDailyDigestShouldSendToSingleAdminWhenOrdersExistToday() {
        ReflectionTestUtils.setField(scheduler, "digestEnabled", true);
        ReflectionTestUtils.setField(scheduler, "adminRecipients", "admin@test.com");

        Order todayOrder = buildOrder(2L, LocalDateTime.now(), "user@test.com", "User One");

        when(orderRepository.findAll()).thenReturn(List.of(todayOrder));

        scheduler.sendDailyDigest();

        verify(orderRepository, times(1)).findAll();
        verify(emailService, times(1))
                .sendDailyOrderDigest(eq("admin@test.com"), contains("Total Orders Today: 1"));
    }

    @Test
    void sendDailyDigestShouldSendToMultipleAdminsWhenOrdersExistToday() {
        ReflectionTestUtils.setField(scheduler, "digestEnabled", true);
        ReflectionTestUtils.setField(scheduler, "adminRecipients", "admin1@test.com, admin2@test.com");

        Order todayOrder1 = buildOrder(3L, LocalDateTime.now(), "user1@test.com", "User One");
        Order todayOrder2 = buildOrder(4L, LocalDateTime.now(), "user2@test.com", "User Two");

        when(orderRepository.findAll()).thenReturn(List.of(todayOrder1, todayOrder2));

        scheduler.sendDailyDigest();

        verify(emailService, times(1))
                .sendDailyOrderDigest(eq("admin1@test.com"), contains("Total Orders Today: 2"));
        verify(emailService, times(1))
                .sendDailyOrderDigest(eq("admin2@test.com"), contains("Total Orders Today: 2"));
    }

    @Test
    void sendDailyDigestShouldNotSendWhenAdminRecipientsBlank() {
        ReflectionTestUtils.setField(scheduler, "digestEnabled", true);
        ReflectionTestUtils.setField(scheduler, "adminRecipients", "   ");

        Order todayOrder = buildOrder(5L, LocalDateTime.now(), "user@test.com", "User One");

        when(orderRepository.findAll()).thenReturn(List.of(todayOrder));

        scheduler.sendDailyDigest();

        verify(orderRepository, times(1)).findAll();
        verify(emailService, never()).sendDailyOrderDigest(anyString(), anyString());
    }

    private Order buildOrder(Long id, LocalDateTime createdAt, String email, String fullName) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(Role.USER);
        user.setEnabled(true);

        Order order = new Order();
        order.setId(id);
        order.setCustomer(user);
        order.setCreatedAt(createdAt);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(999.99));
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));

        return order;
    }
}