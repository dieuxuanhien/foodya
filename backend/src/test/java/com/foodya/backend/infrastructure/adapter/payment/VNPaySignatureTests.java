package com.foodya.backend.infrastructure.adapter.payment;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VNPaySignatureTests {

    @Test
    void hashDataSortsParamsAndEncodesValues() {
        String data = VNPaySignature.hashData(Map.of(
                "vnp_TxnRef", "ORDER-1",
                "vnp_Amount", "6500000",
                "vnp_OrderInfo", "Thanh toan don hang",   // space -> '+' per US-ASCII URL encoding
                "vnp_SecureHash", "ignored"
        ));
        assertEquals("vnp_Amount=6500000&vnp_OrderInfo=Thanh+toan+don+hang&vnp_TxnRef=ORDER-1", data);
    }

    @Test
    void signAndVerifyRoundTrip() {
        Map<String, String> params = Map.of(
                "vnp_Amount", "1000000",
                "vnp_ResponseCode", "00",
                "vnp_TxnRef", "ORDER-X");
        String hash = VNPaySignature.sign(params, "secret123");
        assertEquals(128, hash.length(), "HMAC-SHA512 hex is 128 chars");

        var signed = new java.util.HashMap<>(params);
        signed.put("vnp_SecureHash", hash);
        assertTrue(VNPaySignature.verify(signed, "secret123"));
    }

    @Test
    void verifyRejectsTamperedParamsOrWrongSecret() {
        Map<String, String> params = Map.of("vnp_Amount", "1000000", "vnp_TxnRef", "ORDER-X");
        var signed = new java.util.HashMap<>(params);
        signed.put("vnp_SecureHash", VNPaySignature.sign(params, "secret123"));
        assertFalse(VNPaySignature.verify(signed, "other-secret"));

        var tampered = new java.util.HashMap<>(signed);
        tampered.put("vnp_Amount", "1");
        assertFalse(VNPaySignature.verify(tampered, "secret123"));

        var missing = new java.util.HashMap<>(params);
        assertFalse(VNPaySignature.verify(missing, "secret123"));
    }

    @Test
    void queryStringAppendsSecureHashAndEncodes() {
        String qs = VNPaySignature.queryString(Map.of("vnp_TxnRef", "ORDER-X"), "abc");
        assertEquals("?vnp_SecureHash=abc&vnp_TxnRef=ORDER-X&", qs);
    }
}
