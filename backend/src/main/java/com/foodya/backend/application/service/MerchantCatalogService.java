package com.foodya.backend.application.service;

import com.foodya.backend.domain.persistence.MenuCategory;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.SystemParameter;
import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.CreateRestaurantRequest;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.dto.UpdateRestaurantRequest;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.MenuCategoryPort;
import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.application.port.out.SystemParameterPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class MerchantCatalogService {

    private final RestaurantPort restaurantPort;
    private final MenuCategoryPort menuCategoryPort;
    private final MenuItemPort menuItemPort;
    private final SystemParameterPort systemParameterPort;
    private final PaginationPolicyService paginationPolicyService;
    private final GeoService geoService;

    public MerchantCatalogService(RestaurantPort restaurantPort,
                                  MenuCategoryPort menuCategoryPort,
                                  MenuItemPort menuItemPort,
                                  SystemParameterPort systemParameterPort,
                                  PaginationPolicyService paginationPolicyService,
                                  GeoService geoService) {
        this.restaurantPort = restaurantPort;
        this.menuCategoryPort = menuCategoryPort;
        this.menuItemPort = menuItemPort;
        this.systemParameterPort = systemParameterPort;
        this.paginationPolicyService = paginationPolicyService;
        this.geoService = geoService;
    }

    @Transactional
    public Restaurant createRestaurant(UUID merchantUserId, CreateRestaurantRequest request) {
        validateRestaurantDistance(request.maxDeliveryKm());

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(merchantUserId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineType(request.cuisineType().trim());
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoService.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return restaurantPort.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(UUID merchantUserId, UUID restaurantId, UpdateRestaurantRequest request) {
        validateRestaurantDistance(request.maxDeliveryKm());
        Restaurant restaurant = ownedRestaurant(merchantUserId, restaurantId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineType(request.cuisineType().trim());
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoService.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return restaurantPort.save(restaurant);
    }

    @Transactional
    public MenuCategory createCategory(UUID merchantUserId, UUID restaurantId, CreateMenuCategoryRequest request) {
        ownedRestaurant(merchantUserId, restaurantId);
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCase(restaurantId, request.name().trim())) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurantId);
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return menuCategoryPort.save(category);
    }

    @Transactional(readOnly = true)
    public PaginatedResult<MenuCategory> listCategories(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        return menuCategoryPort.findByRestaurantIdAndActiveTrue(restaurantId, spec.page(), spec.size());
    }

    @Transactional
    public MenuCategory updateCategory(UUID merchantUserId, UUID categoryId, UpdateMenuCategoryRequest request) {
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(category.getRestaurantId(), request.name().trim(), categoryId)) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return menuCategoryPort.save(category);
    }

    @Transactional
    public void deleteCategory(UUID merchantUserId, UUID categoryId) {
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        menuCategoryPort.delete(category);
    }

    @Transactional
    public MenuItem createMenuItem(UUID merchantUserId, UUID restaurantId, CreateMenuItemRequest request) {
        ownedRestaurant(merchantUserId, restaurantId);
        validatePrice(request.price());

        UUID categoryId = parseUuid(request.categoryId(), "categoryId");
        menuCategoryPort.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ValidationException("invalid category", Map.of("categoryId", "does not belong to restaurant")));

        MenuItem item = new MenuItem();
        item.setRestaurantId(restaurantId);
        item.setCategoryId(categoryId);
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setActive(request.isActive());
        item.setAvailable(request.isAvailable());
        return menuItemPort.save(item);
    }

    @Transactional(readOnly = true)
    public PaginatedResult<MenuItem> listMenuItems(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        return menuItemPort.findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(restaurantId, spec.page(), spec.size());
    }

    @Transactional
    public MenuItem updateMenuItem(UUID merchantUserId, UUID menuItemId, UpdateMenuItemRequest request) {
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        validatePrice(request.price());

        UUID categoryId = parseUuid(request.categoryId(), "categoryId");
        menuCategoryPort.findByIdAndRestaurantId(categoryId, item.getRestaurantId())
                .orElseThrow(() -> new ValidationException("invalid category", Map.of("categoryId", "does not belong to restaurant")));

        item.setCategoryId(categoryId);
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setActive(request.isActive());
        item.setAvailable(request.isAvailable());
        return menuItemPort.save(item);
    }

    @Transactional
    public void softDeleteMenuItem(UUID merchantUserId, UUID menuItemId) {
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        item.setDeletedAt(OffsetDateTime.now());
        item.setActive(false);
        menuItemPort.save(item);
    }

    @Transactional
    public MenuItem updateAvailability(UUID merchantUserId, UUID menuItemId, UpdateMenuItemAvailabilityRequest request) {
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        item.setAvailable(request.isAvailable());
        return menuItemPort.save(item);
    }

    private Restaurant ownedRestaurant(UUID merchantUserId, UUID restaurantId) {
        Restaurant restaurant = restaurantPort.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));
        if (!restaurant.getOwnerUserId().equals(merchantUserId)) {
            throw new ForbiddenException("merchant can only manage owned restaurant");
        }
        return restaurant;
    }

    private void validateRestaurantDistance(BigDecimal maxDeliveryKm) {
        if (maxDeliveryKm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("invalid maxDeliveryKm", Map.of("maxDeliveryKm", "must be > 0"));
        }

        BigDecimal platformMax = systemParameterPort.findById("shipping.max_delivery_km")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(BigDecimal.valueOf(15));

        if (maxDeliveryKm.compareTo(platformMax) > 0) {
            throw new ValidationException("invalid maxDeliveryKm", Map.of("maxDeliveryKm", "must be <= shipping.max_delivery_km"));
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("invalid menu item price", Map.of("price", "must be > 0"));
        }
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }
}
