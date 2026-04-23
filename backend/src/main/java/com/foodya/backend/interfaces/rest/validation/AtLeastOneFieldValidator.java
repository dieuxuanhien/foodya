package com.foodya.backend.interfaces.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class AtLeastOneFieldValidator implements ConstraintValidator<AtLeastOneField, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneField annotation) {
        this.fields = annotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        for (String field : fields) {
            try {
                Method accessor = value.getClass().getMethod(field);
                if (accessor.invoke(value) != null) {
                    return true;
                }
            } catch (ReflectiveOperationException ex) {
                return false;
            }
        }
        return false;
    }
}
