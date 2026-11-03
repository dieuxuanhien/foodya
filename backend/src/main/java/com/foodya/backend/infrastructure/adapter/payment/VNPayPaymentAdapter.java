package com.foodya.backend.infrastructure.adapter.payment;

import com.foodya.backend.application.dto.PaymentGatewayDtos.*;
import com.foodya.backend.application.ports.out.PaymentGatewayPort;
import com.foodya.backend.infrastructure.config.VNPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class VNPayPaymentAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(VNPayPaymentAdapter.class);
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SUCCESS_CODE = "00";

    private final VNPayProperties properties;

    public VNPayPaymentAdapter(VNPayProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentInitResult initiate(PaymentInitRequest request) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("VNPAY is not configured (foodya.vnpay.tmn-code / hash-secret)");
        }
        String txnRef = request.orderCode() + "-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now(VN_ZONE);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        // VNPAY expects the amount in VND × 100.
        params.put("vnp_Amount", request.amount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", request.orderInfo());
        params.put("vnp_OrderType", "food_delivery");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", request.returnUrl() != null ? request.returnUrl() : properties.getReturnUrl());
        params.put("vnp_IpAddr", request.ipAddress() == null ? "127.0.0.1" : request.ipAddress());
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNP_DATE));

        String secureHash = VNPaySignature.sign(params, properties.getHashSecret());
        String paymentUrl = properties.getUrl() + VNPaySignature.queryString(params, secureHash);
        return new PaymentInitResult(paymentUrl, txnRef, null);
    }

    @Override
    public PaymentVerifyResult verifyCallback(Map<String, String> params) {
        boolean verified = properties.isConfigured() && VNPaySignature.verify(params, properties.getHashSecret());
        String responseCode = params.get("vnp_ResponseCode");
        BigDecimal amount = parseAmount(params.get("vnp_Amount"));
        boolean success = verified && SUCCESS_CODE.equals(responseCode);
        if (!verified) {
            log.warn("VNPAY callback signature verification failed for txnRef {}", params.get("vnp_TxnRef"));
        }
        return new PaymentVerifyResult(verified, success, params.get("vnp_TxnRef"),
                params.get("vnp_TransactionNo"), responseCode, amount, params.get("vnp_BankCode"));
    }

    /**
     * Query is served from the local transaction record: VNPAY's QueryDR API requires additional
     * merchant configuration and signed requests, so the IPN callback is the source of truth here.
     */
    @Override
    public PaymentQueryResult queryTransaction(String transactionRef) {
        return new PaymentQueryResult("UNKNOWN", null, null);
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw).movePointLeft(2);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
