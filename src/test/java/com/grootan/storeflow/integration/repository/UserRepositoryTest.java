package com.grootan.storeflow.integration.repository;

import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest extends TestContainerConfig {

    @Autowired
    private UserRepository userRepository;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldSaveValidUser() {
        User user = new User();
        user.setEmail("testuser@gmail.com");
        user.setPassword("password123");
        user.setFullName("Test User");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("testuser@gmail.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullName());
        assertTrue(savedUser.isEnabled());
    }

    @Test
    void shouldFailValidationForInvalidEmail() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setPassword("password123");
        user.setFullName("Test User");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailValidationForShortPassword() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("123");
        user.setFullName("Test User");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailValidationForShortFullName() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("password123");
        user.setFullName("A");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = new User();
        user1.setEmail("same@gmail.com");
        user1.setPassword("password123");
        user1.setFullName("User One");
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setEmail("same@gmail.com");
        user2.setPassword("password123");
        user2.setFullName("User Two");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
    }

    @Test
    void shouldFindUserByEmailIgnoreCase() {
        User user = new User();
        user.setEmail("findme@gmail.com");
        user.setPassword("password123");
        user.setFullName("Find Me");
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmailIgnoreCase("FINDME@GMAIL.COM");

        assertTrue(found.isPresent());
        assertEquals("findme@gmail.com", found.get().getEmail());
    }

    @Test
    void resetTokenShouldBeValidWhenTokenExistsAndNotExpired() {
        User user = new User();
        user.setEmail("reset@gmail.com");
        user.setPassword("password123");
        user.setFullName("Reset User");
        user.setResetToken("token123");
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));

        assertTrue(user.isResetTokenValid());
    }

    @Test
    void resetTokenShouldBeInvalidWhenExpired() {
        User user = new User();
        user.setEmail("reset@gmail.com");
        user.setPassword("password123");
        user.setFullName("Reset User");
        user.setResetToken("token123");
        user.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(10));

        assertFalse(user.isResetTokenValid());
    }
}