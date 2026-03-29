package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.dto.NotificationPayload;
import com.grootan.storeflow.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendOrderStatusUpdate(Long orderId, Long userId, NotificationPayload payload) {
        messagingTemplate.convertAndSend(
                "/topic/orders/" + orderId + "/status",
                payload
        );

        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                payload
        );
    }
}