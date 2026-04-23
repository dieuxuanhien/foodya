package com.foodya.backend.application.ports.out;

public interface SecurityPolicyPort {

    String jwtSecret();

    long accessTokenMinutes();

    long refreshTokenDays();

    long resetTokenMinutes();

    long otpExpiryMinutes();
}
