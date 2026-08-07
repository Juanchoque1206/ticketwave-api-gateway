package com.ticketwave.infrastructure.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    public static String generate(String orderId, String ticketId, String eventId) {
        String raw = orderId + ":" + ticketId + ":" + eventId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            String hex = HexFormat.of().formatHex(hash);
            return "TW-" + hex.substring(0, 32).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}