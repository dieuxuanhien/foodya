package com.foodya.backend.infrastructure.adapter.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentRefundRequest;
import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentRefundResult;
import com.foodya.backend.infrastructure.config.VNPayProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VNPayPaymentAdapterTest {

    private static PaymentRefundRequest refundRequest() {
        return new PaymentRefundRequest("ORD-1-abcd1234", 65000, "20261104120000", null,
                "admin", "1.2.3.4", "customer cancelled");
    }

    @Test
    void logOnlyRefundSucceedsWithoutCredentialsOrNetwork() {
        // default mode is LOG_ONLY and nothing is configured: a refund must still be recorded as done
        VNPayPaymentAdapter adapter = new VNPayPaymentAdapter(new VNPayProperties(), new ObjectMapper());

        PaymentRefundResult result = adapter.refund(refundRequest());

        assertThat(result.success()).isTrue();
        assertThat(result.responseCode()).isEqualTo("LOG");
        assertThat(result.gatewayTransactionNo()).isNull();
    }

    @Test
    void gatewayModeRefusesToRunWithoutCredentials() {
        VNPayProperties properties = new VNPayProperties();
        properties.setRefundMode("GATEWAY");
        VNPayPaymentAdapter adapter = new VNPayPaymentAdapter(properties, new ObjectMapper());

        assertThatThrownBy(() -> adapter.refund(refundRequest())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownRefundModeFallsBackToLogOnlyRatherThanMovingMoney() {
        VNPayProperties properties = new VNPayProperties();
        properties.setRefundMode("whatever");

        assertThat(properties.isRefundLogOnly()).isTrue();
    }

    @Test
    void clientAddressesVnpayRejectsBecomeLoopback() {
        assertThat(VNPayPaymentAdapter.cleanIp("0:0:0:0:0:0:0:1")).isEqualTo("127.0.0.1");
        assertThat(VNPayPaymentAdapter.cleanIp("::1")).isEqualTo("127.0.0.1");
        assertThat(VNPayPaymentAdapter.cleanIp(null)).isEqualTo("127.0.0.1");
        assertThat(VNPayPaymentAdapter.cleanIp("  ")).isEqualTo("127.0.0.1");
    }

    @Test
    void ipv4AndIpv4MappedAddressesArePassedThrough() {
        assertThat(VNPayPaymentAdapter.cleanIp("203.0.113.9")).isEqualTo("203.0.113.9");
        assertThat(VNPayPaymentAdapter.cleanIp("::ffff:192.168.1.10")).isEqualTo("192.168.1.10");
    }
}