package com.foodya.backend.domain.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Pure Java domain service for generating standardized business order codes (FOOD-YYYYMMDD-XXXX).
 */
public class OrderCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderCodeGenerator() {
        // Utility class
    }

    /**
     * Generates a new order code with current date and random 4-char suffix.
     * Format: FOOD-YYYYMMDD-XXXX
     */
    public static String generateOrderCode() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "FOOD-" + dateStr + "-" + suffix;
    }

    /**
     * Generates an order code for a given date and sequential number.
     * Format: FOOD-YYYYMMDD-XXXX
     */
    public static String generateOrderCode(LocalDate date, int sequenceNumber) {
        String dateStr = (date != null ? date : LocalDate.now()).format(DATE_FORMATTER);
        String suffix = String.format("%04d", Math.abs(sequenceNumber) % 10000);
        return "FOOD-" + dateStr + "-" + suffix;
    }
}
