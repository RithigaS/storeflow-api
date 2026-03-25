package com.grootan.storeflow.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
}