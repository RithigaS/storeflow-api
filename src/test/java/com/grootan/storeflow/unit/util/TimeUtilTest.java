package com.grootan.storeflow.unit.util;

import com.grootan.storeflow.util.TimeUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilTest {

    @Test
    void currentTimestamp_shouldReturnNonNullIsoString() {
        String timestamp = TimeUtil.currentTimestamp();

        assertNotNull(timestamp);
        assertTrue(timestamp.contains("T"));
        assertTrue(timestamp.endsWith("Z"));
    }

    @Test
    void currentTimestamp_shouldReturnDifferentValuesAcrossCalls() throws InterruptedException {
        String first = TimeUtil.currentTimestamp();
        Thread.sleep(5);
        String second = TimeUtil.currentTimestamp();

        assertNotEquals(first, second);
    }
}