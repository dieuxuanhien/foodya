package com.foodya.backend.infrastructure.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Default {@link RedisCacheAdapter.JsonCodec} backed by the application ObjectMapper. */
public class JacksonJsonCodec implements RedisCacheAdapter.JsonCodec {

    private final ObjectMapper objectMapper;

    public JacksonJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize cache value", ex);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize cache value", ex);
        }
    }
}
