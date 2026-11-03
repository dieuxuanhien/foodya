package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.out.TokenPort;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.domain.value_objects.*;
import com.foodya.backend.infrastructure.adapter.payment.VNPaySignature;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.persistence.models.*;
import com.foodya.backend.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "foodya.vnpay.tmn-code=TESTTMN",
        "foodya.vnpay.hash-secret=test-hash-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTests {

    private static final String SECRET = "test-hash-secret";

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenPort tokenService;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private OrderManagementRepository orderManagementRepository;

    private UserAccount customer;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customer = seedUser("pay-cust", UserRole.CUSTOMER);

        RestaurantPersistenceModel restaurant = new RestaurantPersistenceModel();
        restaurant.setId(UUID.randomUUID());
        restaurant.setOwnerUserId(customer.getId());
        restaurant.setName("Pay Restaurant");
        restaurant.setCuisineType("vietnamese");
        restaurant.setAddressLine("1 Pay St");
        restaurant.setLatitude(new BigDecimal("10.762622"));
        restaurant.setLongitude(new BigDecimal("106.660172"));
        restaurant.setH3IndexRes9("8929a4825ffffff");
        restaurant.setMaxDeliveryKm(new BigDecimal("5.0"));
        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurant.setCreatedAt(OffsetDateTime.now());
        restaurant = restaurantRepository.save(restaurant);

        OrderPersistenceModel order = new OrderPersistenceModel();
        order.setId(UUID.randomUUID());
        order.setOrderCode("PAY-" + System.nanoTime());
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setCustomerUserId(customer.getId());
        order.setRestaurantId(restaurant.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress("2 Customer St");
        order.setDeliveryLatitude(new BigDecimal("10.772622"));
        order.setDeliveryLongitude(new BigDecimal("106.670172"));
        order.setSubtotalAmount(new BigDecimal("50000"));
        order.setDeliveryFee(new BigDecimal("15000"));
        order.setTotalAmount(new BigDecimal("65000"));
        order.setCommissionAmount(BigDecimal.ZERO);
        order.setShippingFeeMarginAmount(BigDecimal.ZERO);
        order.setPlatformProfitAmount(BigDecimal.ZERO);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPlacedAt(OffsetDateTime.now());
        orderId = orderManagementRepository.save(order).getId();
    }

    @Test
    void initiateReturnsSandboxPaymentUrlAndStatusIsPending() throws Exception {
        String jwt = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/customer/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"VNPAY_QR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentUrl").value(
                        org.hamcrest.Matchers.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?")))
                .andExpect(jsonPath("$.data.transactionRef").isNotEmpty());

        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/payment-status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.data.orderPaymentStatus").value("UNPAID"));
    }

    @Test
    void codInitiationIsRejected() throws Exception {
        String jwt = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());
        mockMvc.perform(post("/api/v1/customer/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void signedIpnMarksOrderPaidAndDuplicateIsIdempotent() throws Exception {
        String jwt = tokenService.issueAccessToken(customer, UUID.randomUUID().toString());
        var resp = mockMvc.perform(post("/api/v1/customer/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"VNPAY_QR\"}"))
                .andReturn().getResponse().getContentAsString();
        String txnRef = com.jayway.jsonpath.JsonPath.read(resp, "$.data.transactionRef");

        var ipn = signedIpn(txnRef, "00");
        mockMvc.perform(get("/api/v1/payments/vnpay/callback").params(ipn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));
        // replay
        mockMvc.perform(get("/api/v1/payments/vnpay/callback").params(ipn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/payment-status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderPaymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.transactionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.paymentMethod").value("VNPAY"));
    }

    @Test
    void tamperedIpnIsRejected() throws Exception {
        var ipn = signedIpn("does-not-exist", "00");
        ipn.set("vnp_SecureHash", "deadbeef".repeat(16));
        mockMvc.perform(get("/api/v1/payments/vnpay/callback").params(ipn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    private org.springframework.util.LinkedMultiValueMap<String, String> signedIpn(String txnRef, String responseCode) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "TESTTMN");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionNo", "14000001");
        params.put("vnp_Amount", "6500000");
        params.put("vnp_BankCode", "NCB");
        var mv = new org.springframework.util.LinkedMultiValueMap<String, String>();
        params.forEach(mv::add);
        mv.add("vnp_SecureHash", VNPaySignature.sign(params, SECRET));
        return mv;
    }

    private UserAccount seedUser(String stem, UserRole role) {
        UserAccountPersistenceModel model = new UserAccountPersistenceModel();
        model.setUsername(stem + "-" + System.nanoTime());
        model.setEmail(stem + "-" + System.nanoTime() + "@test.local");
        model.setPhoneNumber("+8490" + String.format("%07d", Math.abs(System.nanoTime() % 10000000)));
        model.setFullName(stem);
        model.setRole(role);
        model.setStatus(UserStatus.ACTIVE);
        model.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return userAccountMapper.toDomain(userAccountRepository.save(model));
    }
}
