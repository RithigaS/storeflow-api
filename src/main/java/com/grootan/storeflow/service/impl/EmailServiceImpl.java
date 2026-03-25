package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("Sending reset password email to {} with link {}", to, resetLink);
    }
}