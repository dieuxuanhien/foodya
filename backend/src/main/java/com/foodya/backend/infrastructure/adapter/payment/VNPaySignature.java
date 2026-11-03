package com.foodya.backend.infrastructure.adapter.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPAY v2.1.0 signing helpers: HMAC-SHA512 over the query string of alphabetically sorted,
 * URL-encoded params (US-ASCII), excluding {@code vnp_SecureHash*}.
 */
public final class VNPaySignature {

    private VNPaySignature() {}

    public static String hashData(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !e.getKey().startsWith("vnp_SecureHash"))
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    public static String queryString(Map<String, String> params, String secureHash) {
        Map<String, String> signed = new TreeMap<>(params);
        signed.put("vnp_SecureHash", secureHash);
        return signed.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&", "?", "&"));
    }

    public static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot compute HMAC-SHA512", ex);
        }
    }

    public static String sign(Map<String, String> params, String secret) {
        return hmacSha512(secret, hashData(params));
    }

    public static boolean verify(Map<String, String> params, String secret) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;
        String expected = sign(params, secret);
        return constantTimeEquals(expected, received);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
