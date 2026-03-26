package com.grootan.storeflow.dto;

import java.time.LocalDateTime;

public class NotificationPayload {

    private String message;
    private String status;
    private LocalDateTime timestamp;

    public NotificationPayload(String message, String status, LocalDateTime timestamp) {
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}