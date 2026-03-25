package com.grootan.storeflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {}

    public Long getId() { return id; }

    public String getToken() { return token; }

    public User getUser() { return user; }

    public LocalDateTime getExpiryDate() { return expiryDate; }

    public boolean isUsed() { return used; }

    public void setId(Long id) { this.id = id; }

    public void setToken(String token) { this.token = token; }

    public void setUser(User user) { this.user = user; }

    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public void setUsed(boolean used) { this.used = used; }
}