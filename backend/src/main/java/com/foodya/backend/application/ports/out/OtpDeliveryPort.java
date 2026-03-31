package com.foodya.backend.application.ports.out;

public interface OtpDeliveryPort {

    void sendPasswordResetOtp(String email, String otp);
}
