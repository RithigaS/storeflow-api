package com.grootan.storeflow.unit.websocket;

import com.grootan.storeflow.dto.NotificationPayload;
import com.grootan.storeflow.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void sends_to_topic_and_user_queue() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        NotificationServiceImpl service = new NotificationServiceImpl(template);

        NotificationPayload payload =
                new NotificationPayload("msg","SHIPPED",java.time.LocalDateTime.now());

        service.sendOrderStatusUpdate(1L, 5L, payload);

        verify(template).convertAndSend("/topic/orders/1/status", payload);
        verify(template).convertAndSendToUser("5", "/queue/notifications", payload);
    }
}