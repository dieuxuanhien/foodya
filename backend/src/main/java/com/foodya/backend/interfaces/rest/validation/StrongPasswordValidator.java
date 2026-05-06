package com.foodya.backend.interfaces.rest.validation;

import com.foodya.backend.domain.policies.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches(PasswordPolicy.REGEX);
    }
}
