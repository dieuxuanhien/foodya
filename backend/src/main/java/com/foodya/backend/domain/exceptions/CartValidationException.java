package com.foodya.backend.domain.exceptions;

public class CartValidationException extends IllegalStateException {

    public CartValidationException(String message) {
        super(message);
    }
}