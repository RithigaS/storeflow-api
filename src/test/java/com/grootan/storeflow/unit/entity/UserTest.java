package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldTrimAndLowercaseEmailWhenNotNull() {
        User user = new User();

        user.setEmail("  RITHI@GMAIL.COM  ");

        assertEquals("rithi@gmail.com", user.getEmail());
    }

    @Test
    void shouldSetEmailToNullWhenInputIsNull() {
        User user = new User();

        user.setEmail(null);

        assertNull(user.getEmail());
    }

    @Test
    void shouldTrimFullNameWhenNotNull() {
        User user = new User();

        user.setFullName("  Rithi  ");

        assertEquals("Rithi", user.getFullName());
    }

    @Test
    void shouldSetFullNameToNullWhenInputIsNull() {
        User user = new User();

        user.setFullName(null);

        assertNull(user.getFullName());
    }

    @Test
    void shouldReturnFalseWhenResetTokenIsNull() {
        User user = new User();
        user.setResetToken(null);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        assertFalse(user.isResetTokenValid());
    }

    @Test
    void shouldReturnFalseWhenResetTokenExpiryIsNull() {
        User user = new User();
        user.setResetToken("token123");
        user.setResetTokenExpiresAt(null);

        assertFalse(user.isResetTokenValid());
    }

    @Test
    void shouldReturnFalseWhenResetTokenIsExpired() {
        User user = new User();
        user.setResetToken("token123");
        user.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(5));

        assertFalse(user.isResetTokenValid());
    }

    @Test
    void shouldReturnTrueWhenResetTokenAndExpiryAreValid() {
        User user = new User();
        user.setResetToken("token123");
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertTrue(user.isResetTokenValid());
    }

    @Test
    void shouldCoverAllRemainingGettersAndSetters() {
        User user = new User();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        user.setId(1L);
        user.setPassword("secret123");
        user.setRole(Role.ADMIN);
        user.setAvatarPath("/avatars/user.png");
        user.setResetToken("reset-token");
        user.setResetTokenExpiresAt(expiry);
        user.setEnabled(false);

        assertEquals(1L, user.getId());
        assertEquals("secret123", user.getPassword());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("/avatars/user.png", user.getAvatarPath());
        assertEquals("reset-token", user.getResetToken());
        assertEquals(expiry, user.getResetTokenExpiresAt());
        assertFalse(user.isEnabled());
    }
}