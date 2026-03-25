package com.grootan.storeflow.dto.auth;

public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String avatarUrl;

    public UserProfileResponse() {}

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }

    public void setId(Long id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}