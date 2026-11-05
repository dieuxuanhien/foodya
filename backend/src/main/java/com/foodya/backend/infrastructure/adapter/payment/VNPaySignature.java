package com.foodya.backend.infrastructure.adapter.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPAY v2.1.0 signing helpers, matching the official Java sample exactly.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Collect all params except {@code vnp_SecureHash*} and empty values.</li>
 *   <li>Sort alphabetically by the <em>plain</em> (unencoded) key.</li>
 *   <li>Build {@code key=URLEncode(value, US_ASCII)} entries joined with {@code &}.
 *       The key itself is NOT encoded in the hash data.</li>
 *   <li>HMAC-SHA512 the hash data with UTF-8 bytes.</li>
 * </ol>
 *
 * <p>The payment query string URL-encodes both keys and values (keys are ASCII so encoding is a no-op),
 * sorts ascending, and appends {@code vnp_SecureHash} last.
 */
public final class VNPaySignature {

    private VNPaySignature() {}

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII).replace("%20", "+");
    }

    /**
     * Builds the hash data string: sort by plain key ascending, {@code key=encodedValue} joined with {@code &}.
     * Excludes any param whose key starts with {@code vnp_SecureHash}.
     */
    public static String hashData(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().startsWith("vnp_SecureHash"))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Builds the full payment URL query string. Keys and values are URL-encoded, params sorted ascending,
     * and {@code vnp_SecureHash} is appended last (after all other params).
     */
    public static String queryString(Map<String, String> params, String secureHash) {
        String query = new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        if (query.isEmpty()) {
            return "?vnp_SecureHash=" + secureHash;
        }
        return "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
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
        return constantTimeEquals(expected.toLowerCase(), received.toLowerCase());
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