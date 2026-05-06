package com.foodya.backend.interfaces.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
import java.util.Objects;

public class FieldsMatchValidator implements ConstraintValidator<FieldsMatch, Object> {

    private String first;
    private String second;

    @Override
    public void initialize(FieldsMatch annotation) {
        this.first = annotation.first();
        this.second = annotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            Object firstValue = read(value, first);
            Object secondValue = read(value, second);
            boolean matched = Objects.equals(firstValue, secondValue);
            if (!matched) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(second)
                    .addConstraintViolation();
            }
            return matched;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Object read(Object source, String fieldName) throws ReflectiveOperationException {
        Method accessor = source.getClass().getMethod(fieldName);
        return accessor.invoke(source);
    }
}
