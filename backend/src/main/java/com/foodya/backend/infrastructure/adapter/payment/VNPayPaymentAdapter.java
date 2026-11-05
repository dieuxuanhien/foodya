package com.foodya.backend.infrastructure.adapter.payment;

import com.foodya.backend.application.dto.PaymentGatewayDtos.*;
import com.foodya.backend.application.ports.out.PaymentGatewayPort;
import com.foodya.backend.infrastructure.config.VNPayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class VNPayPaymentAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(VNPayPaymentAdapter.class);
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SUCCESS_CODE = "00";

    private final VNPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VNPayPaymentAdapter(VNPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", request.returnUrl() != null ? request.returnUrl() : properties.getReturnUrl());
        params.put("vnp_IpAddr", cleanIp(request.ipAddress()));
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNP_DATE));

        String secureHash = VNPaySignature.sign(params, properties.getHashSecret());
        String paymentUrl = properties.getUrl() + VNPaySignature.queryString(params, secureHash);
        return new PaymentInitResult(paymentUrl, txnRef, params.get("vnp_CreateDate"));
    }

    /**
     * Full refund through VNPAY's merchant web API (JSON POST).
     *
     * <p>The signature is an HMAC-SHA512 over the pipe-delimited field list defined by VNPAY, which is
     * different from the pay-URL signature. {@code request.amount()} is in VND; the gateway wants ×100.
     */
    @Override
    public PaymentRefundResult refund(PaymentRefundRequest request) {
        if (properties.isRefundLogOnly()) {
            log.info("VNPAY refund recorded (log only, no money moved): txnRef={} amount={} by={} reason={}",
                    request.transactionRef(), request.amount(), request.createBy(), request.reason());
            return new PaymentRefundResult(true, "LOG", "refund recorded; no money moved (log-only mode)", null);
        }
        if (!properties.isConfigured()) {
            throw new IllegalStateException("VNPAY is not configured (foodya.vnpay.tmn-code / hash-secret)");
        }
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = LocalDateTime.now(VN_ZONE).format(VNP_DATE);
        String amount = BigDecimal.valueOf(request.amount()).multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        String transactionNo = request.transactionNo() == null ? "" : request.transactionNo();
        String orderInfo = request.reason() == null || request.reason().isBlank() ? "Hoan tien don hang" : request.reason();
        String ip = cleanIp(request.ipAddress());

        String hashData = String.join("|", requestId, "2.1.0", "refund", properties.getTmnCode(), "02",
                request.transactionRef(), amount, transactionNo, request.transactionDate(),
                request.createBy(), createDate, ip, orderInfo);
        String secureHash = VNPaySignature.hmacSha512(properties.getHashSecret(), hashData);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", "2.1.0");
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", properties.getTmnCode());
        body.put("vnp_TransactionType", "02");
        body.put("vnp_TxnRef", request.transactionRef());
        body.put("vnp_Amount", amount);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", transactionNo);
        body.put("vnp_TransactionDate", request.transactionDate());
        body.put("vnp_CreateBy", request.createBy());
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", secureHash);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiUrl()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            String responseCode = json.path("vnp_ResponseCode").asText("");
            String message = json.path("vnp_Message").asText("");
            boolean success = SUCCESS_CODE.equals(responseCode);
            if (!success) {
                log.warn("VNPAY refund rejected for txnRef {}: {} {}", request.transactionRef(), responseCode, message);
            }
            return new PaymentRefundResult(success, responseCode, message, json.path("vnp_TransactionNo").asText(null));
        } catch (Exception ex) {
            log.error("VNPAY refund request failed for txnRef {}", request.transactionRef(), ex);
            return new PaymentRefundResult(false, "99", "Refund request failed: " + ex.getMessage(), null);
        }
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

    private static final java.util.regex.Pattern IPV4 =
            java.util.regex.Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    /**
     * VNPAY only accepts IPv4 in {@code vnp_IpAddr}. Local/netem requests can report IPv6 loopback
     * ("0:0:0:0:0:0:0:1") or IPv4-mapped addresses ("::ffff:192.168.1.10"). Fall back to "127.0.0.1"
     * for anything that isn't a valid dotted-quad, which the VNPAY sandbox accepts.
     */
    static String cleanIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return "127.0.0.1";
        }
        String candidate = raw.trim();
        if (candidate.startsWith("::ffff:")) {
            candidate = candidate.substring(7);
        }
        return IPV4.matcher(candidate).matches() ? candidate : "127.0.0.1";
    }
}
