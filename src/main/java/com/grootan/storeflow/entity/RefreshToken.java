package com.grootan.storeflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    public RefreshToken() {}

    public Long getId() { return id; }

    public String getToken() { return token; }

    public User getUser() { return user; }

    public LocalDateTime getExpiryDate() { return expiryDate; }

    public boolean isRevoked() { return revoked; }

    public void setId(Long id) { this.id = id; }

    public void setToken(String token) { this.token = token; }

    public void setUser(User user) { this.user = user; }

    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}