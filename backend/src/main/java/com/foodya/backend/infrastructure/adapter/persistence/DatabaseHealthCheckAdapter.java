package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.HealthCheckPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthCheckAdapter implements HealthCheckPort {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthCheckAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isDatabaseReady() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}