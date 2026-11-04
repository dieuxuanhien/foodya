package com.foodya.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodya.vnpay")
public class VNPayProperties {

    private String tmnCode;
    private String hashSecret;
    private String url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:8080/api/v1/payments/vnpay/return";
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    /** LOG_ONLY records the refund without calling the gateway; GATEWAY calls the VNPAY refund API. */
    private String refundMode = "LOG_ONLY";

    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank() && hashSecret != null && !hashSecret.isBlank();
    }

    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getRefundMode() { return refundMode; }
    public void setRefundMode(String refundMode) { this.refundMode = refundMode; }
    public boolean isRefundLogOnly() { return !"GATEWAY".equalsIgnoreCase(refundMode); }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
}
