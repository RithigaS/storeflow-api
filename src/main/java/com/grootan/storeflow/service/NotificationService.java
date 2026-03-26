package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.NotificationPayload;

public interface NotificationService {

    void sendOrderStatusUpdate(Long orderId, Long userId, NotificationPayload payload);
}