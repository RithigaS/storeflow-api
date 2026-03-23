package com.grootan.storeflow.dto;

public class HealthResponse {

    private String status;
    private String timestamp;
    private long uptime;

    public HealthResponse() {
    }

    public HealthResponse(String status, String timestamp, long uptime) {
        this.status = status;
        this.timestamp = timestamp;
        this.uptime = uptime;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public long getUptime() {
        return uptime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }
}