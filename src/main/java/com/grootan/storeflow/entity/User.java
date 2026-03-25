package com.grootan.storeflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grootan.storeflow.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(nullable = false)
    private boolean enabled = true;

    public User() {
    }

    // Check reset token validity
    @Transient
    public boolean isResetTokenValid() {
        return resetToken != null
                && resetTokenExpiresAt != null
                && resetTokenExpiresAt.isAfter(LocalDateTime.now());
    }

    // For Spring Security (ROLE_USER / ROLE_ADMIN)
    @Transient
    public String getAuthority() {
        return "ROLE_" + this.role.name();
    }

    // Clear token after password reset
    public void clearResetToken() {
        this.resetToken = null;
        this.resetTokenExpiresAt = null;
    }

    // GETTERS

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarPath() {
        return avatarUrl;
    }

    public String getResetToken() {
        return resetToken;
    }

    public LocalDateTime getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // SETTERS

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public void setRole(Role role) {
        this.role = (role == null) ? Role.USER : role;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarUrl = avatarPath;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}