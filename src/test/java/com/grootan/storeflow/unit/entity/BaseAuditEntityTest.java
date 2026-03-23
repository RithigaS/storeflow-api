package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.BaseAuditEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseAuditEntityTest {

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnCreate() {
        TestAuditEntity entity = new TestAuditEntity();

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
        assertTrue(entity.beforeCreateCalled);
    }

    @Test
    void shouldUpdateOnlyUpdatedAtOnUpdate() throws InterruptedException {
        TestAuditEntity entity = new TestAuditEntity();

        entity.onCreate();
        LocalDateTime createdAt = entity.getCreatedAt();
        LocalDateTime firstUpdatedAt = entity.getUpdatedAt();

        Thread.sleep(5);

        entity.onUpdate();

        assertEquals(createdAt, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(firstUpdatedAt) || entity.getUpdatedAt().isEqual(firstUpdatedAt));
        assertTrue(entity.beforeUpdateCalled);
    }

    @Test
    void shouldCoverAuditSetters() {
        TestAuditEntity entity = new TestAuditEntity();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
    }

    static class TestAuditEntity extends BaseAuditEntity {
        boolean beforeCreateCalled = false;
        boolean beforeUpdateCalled = false;

        @Override
        protected void beforeCreate() {
            beforeCreateCalled = true;
        }

        @Override
        protected void beforeUpdate() {
            beforeUpdateCalled = true;
        }
    }
}