package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.UpdateRestaurantRequest;
import com.foodya.backend.interfaces.rest.validation.AtLeastOneField;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@AtLeastOneField(fields = {"cuisineType", "cuisineTypes"}, message = "either cuisineType or cuisineTypes must be provided")
public record UpdateRestaurantApiRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String cuisineType,
        @Size(max = 20) List<@NotBlank @Size(max = 100) String> cuisineTypes,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String addressLine,
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
        @NotNull @DecimalMin(value = "0.1") BigDecimal maxDeliveryKm,
        @NotNull Boolean isOpen
) {
    public UpdateRestaurantRequest toApplicationDto() {
        return new UpdateRestaurantRequest(
                name, cuisineType, cuisineTypes, description, addressLine, latitude, longitude, maxDeliveryKm, isOpen
        );
    }
}
