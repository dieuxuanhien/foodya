package com.foodya.backend.application.ports.out;

public interface PasswordHashPort {

    String encode(String rawValue);

    boolean matches(String rawValue, String encodedValue);
}
