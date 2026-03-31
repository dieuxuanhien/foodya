package com.foodya.backend.application.port.out;

public interface SecurityPolicyPort {

    String jwtSecret();

    long accessTokenMinutes();

    long refreshTokenDays();

    long resetTokenMinutes();

    long otpExpiryMinutes();
}
