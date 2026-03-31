package com.foodya.backend.infrastructure.adapter.security;

import com.foodya.backend.application.ports.out.PasswordHashPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashAdapter implements PasswordHashPort {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawValue) {
        return passwordEncoder.encode(rawValue);
    }

    @Override
    public boolean matches(String rawValue, String encodedValue) {
        return passwordEncoder.matches(rawValue, encodedValue);
    }
}
