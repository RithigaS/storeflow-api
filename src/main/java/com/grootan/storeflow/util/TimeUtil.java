package com.grootan.storeflow.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private TimeUtil() {
    }

    public static String currentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }
}