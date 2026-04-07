package com.foodya.backend.application.usecases;

import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.CreateRestaurantRequest;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.dto.UpdateRestaurantRequest;
import com.foodya.backend.application.constants.AppCuisineCatalog;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.MerchantCatalogUseCase;
import com.foodya.backend.application.ports.out.MenuCategoryPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.ports.out.MenuItemImageStoragePort;
import com.foodya.backend.application.ports.out.RestaurantImageStoragePort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.application.support.PaginationPolicy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MerchantCatalogService implements MerchantCatalogUseCase {

    private final RestaurantPort restaurantPort;
    private final MenuCategoryPort menuCategoryPort;
    private final MenuItemPort menuItemPort;
    private final SystemParameterPort systemParameterPort;
    private final PaginationPolicy paginationPolicy;
    private final GeoPort geoPort;
    private final MenuItemImageStoragePort menuItemImageStoragePort;
    private final RestaurantImageStoragePort restaurantImageStoragePort;

    public MerchantCatalogService(RestaurantPort restaurantPort,
                                  MenuCategoryPort menuCategoryPort,
                                  MenuItemPort menuItemPort,
                                  SystemParameterPort systemParameterPort,
                                  PaginationPolicy paginationPolicy,
                                  GeoPort geoPort,
                                  MenuItemImageStoragePort menuItemImageStoragePort,
                                  RestaurantImageStoragePort restaurantImageStoragePort) {
        this.restaurantPort = restaurantPort;
        this.menuCategoryPort = menuCategoryPort;
        this.menuItemPort = menuItemPort;
        this.systemParameterPort = systemParameterPort;
        this.paginationPolicy = paginationPolicy;
        this.geoPort = geoPort;
        this.menuItemImageStoragePort = menuItemImageStoragePort;
        this.restaurantImageStoragePort = restaurantImageStoragePort;
    }

    public RestaurantData createRestaurant(UUID merchantUserId, CreateRestaurantRequest request) {
        requireText(request.name(), "name");
        requireText(request.addressLine(), "addressLine");
        requireNonNull(request.latitude(), "latitude");
        requireNonNull(request.longitude(), "longitude");
        requireNonNull(request.maxDeliveryKm(), "maxDeliveryKm");
        requireNonNull(request.isOpen(), "isOpen");
        validateRestaurantDistance(request.maxDeliveryKm());
        List<String> cuisineTypes = resolveCuisineTypes(request.cuisineTypes(), request.cuisineType());

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(merchantUserId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineTypes(cuisineTypes);
        restaurant.setCuisineType(cuisineTypes.get(0));
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoPort.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return toRestaurantData(restaurantPort.save(restaurant));
    }

    public RestaurantData updateRestaurant(UUID merchantUserId, UUID restaurantId, UpdateRestaurantRequest request) {
        requireText(request.name(), "name");
        requireText(request.addressLine(), "addressLine");
        requireNonNull(request.latitude(), "latitude");
        requireNonNull(request.longitude(), "longitude");
        requireNonNull(request.maxDeliveryKm(), "maxDeliveryKm");
        requireNonNull(request.isOpen(), "isOpen");
        validateRestaurantDistance(request.maxDeliveryKm());
        List<String> cuisineTypes = resolveCuisineTypes(request.cuisineTypes(), request.cuisineType());
        Restaurant restaurant = ownedRestaurant(merchantUserId, restaurantId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineTypes(cuisineTypes);
        restaurant.setCuisineType(cuisineTypes.get(0));
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoPort.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return toRestaurantData(restaurantPort.save(restaurant));
    }

    public RestaurantData uploadRestaurantImage(UUID merchantUserId,
                                                UUID restaurantId,
                                                String originalFileName,
                                                String contentType,
                                                byte[] content) {
        requireText(originalFileName, "fileName");
        requireNonNull(content, "content");
        validateImageContent(contentType, content.length);

        Restaurant restaurant = ownedRestaurant(merchantUserId, restaurantId);
        String imageUrl = restaurantImageStoragePort.store(restaurantId, originalFileName, contentType, content);
        restaurant.setImageUrl(imageUrl);
        return toRestaurantData(restaurantPort.save(restaurant));
    }

    public MenuCategoryData createCategory(UUID merchantUserId, UUID restaurantId, CreateMenuCategoryRequest request) {
        requireText(request.name(), "name");
        requireNonNull(request.sortOrder(), "sortOrder");
        requireNonNull(request.isActive(), "isActive");
        ownedRestaurant(merchantUserId, restaurantId);
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCase(restaurantId, request.name().trim())) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurantId);
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return toMenuCategoryData(menuCategoryPort.save(category));
    }

    public PaginatedResult<MenuCategoryData> listCategories(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<MenuCategory> result = menuCategoryPort.findByRestaurantIdAndActiveTrue(restaurantId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuCategoryData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public MenuCategoryData updateCategory(UUID merchantUserId, UUID categoryId, UpdateMenuCategoryRequest request) {
        requireText(request.name(), "name");
        requireNonNull(request.sortOrder(), "sortOrder");
        requireNonNull(request.isActive(), "isActive");
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(category.getRestaurantId(), request.name().trim(), categoryId)) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return toMenuCategoryData(menuCategoryPort.save(category));
    }

    public void deleteCategory(UUID merchantUserId, UUID categoryId) {
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        menuCategoryPort.delete(category);
    }

    public MenuItemData createMenuItem(UUID merchantUserId, UUID restaurantId, CreateMenuItemRequest request) {
        requireText(request.categoryId(), "categoryId");
        requireText(request.name(), "name");
        requireText(request.description(), "description");
        requireNonNull(request.price(), "price");
        requireNonNull(request.isActive(), "isActive");
        requireNonNull(request.isAvailable(), "isAvailable");
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
        return toMenuItemData(menuItemPort.save(item));
    }

    public PaginatedResult<MenuItemData> listMenuItems(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicy.PaginationSpec spec = paginationPolicy.page(page, size);
        PaginatedResult<MenuItem> result = menuItemPort.findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(restaurantId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuItemData).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public MenuItemData updateMenuItem(UUID merchantUserId, UUID menuItemId, UpdateMenuItemRequest request) {
        requireText(request.categoryId(), "categoryId");
        requireText(request.name(), "name");
        requireText(request.description(), "description");
        requireNonNull(request.price(), "price");
        requireNonNull(request.isActive(), "isActive");
        requireNonNull(request.isAvailable(), "isAvailable");
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
        return toMenuItemData(menuItemPort.save(item));
    }

    public void softDeleteMenuItem(UUID merchantUserId, UUID menuItemId) {
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        item.setDeletedAt(OffsetDateTime.now());
        item.setActive(false);
        menuItemPort.save(item);
    }

    public MenuItemData updateAvailability(UUID merchantUserId, UUID menuItemId, UpdateMenuItemAvailabilityRequest request) {
        requireNonNull(request.isAvailable(), "isAvailable");
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        item.setAvailable(request.isAvailable());
        return toMenuItemData(menuItemPort.save(item));
    }

    public MenuItemData uploadMenuItemImage(UUID merchantUserId,
                                            UUID menuItemId,
                                            String originalFileName,
                                            String contentType,
                                            byte[] content) {
        requireText(originalFileName, "fileName");
        requireNonNull(content, "content");
        validateImageContent(contentType, content.length);

        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());

        String imageUrl = menuItemImageStoragePort.store(menuItemId, originalFileName, contentType, content);
        item.setImageUrl(imageUrl);
        return toMenuItemData(menuItemPort.save(item));
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

    private void validatePrice(BigDecimal price) {
        BigDecimal minPrice = systemParameterPort.findById("catalog.menu_item_price_min")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ONE);
        BigDecimal maxPrice = systemParameterPort.findById("catalog.menu_item_price_max")
                .map(SystemParameter::getValue)
                .map(BigDecimal::new)
                .orElse(new BigDecimal("10000000"));

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("invalid menu item price", Map.of("price", "must be > 0"));
        }
        if (price.compareTo(minPrice) < 0 || price.compareTo(maxPrice) > 0) {
            throw new ValidationException(
                    "invalid menu item price",
                    Map.of("price", "must be between " + minPrice.toPlainString() + " and " + maxPrice.toPlainString())
            );
        }
    }

    private static void validateImageContent(String contentType, int contentLength) {
        if (contentType != null && !contentType.isBlank() && !contentType.startsWith("image/")) {
            throw new ValidationException("invalid contentType", Map.of("contentType", "must be an image mime type"));
        }
        if (contentLength <= 0) {
            throw new ValidationException("invalid content", Map.of("file", "must not be empty"));
        }
        int maxBytes = 5 * 1024 * 1024;
        if (contentLength > maxBytes) {
            throw new ValidationException("invalid content", Map.of("file", "must be <= 5MB"));
        }
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("invalid " + field, Map.of(field, "must not be blank"));
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException("invalid " + field, Map.of(field, "must not be null"));
        }
    }

    private static List<String> resolveCuisineTypes(List<String> cuisineTypes, String legacyCuisineType) {
        List<String> selected = cuisineTypes;
        if ((selected == null || selected.isEmpty()) && legacyCuisineType != null && !legacyCuisineType.isBlank()) {
            selected = List.of(legacyCuisineType);
        }
        if (selected == null || selected.isEmpty()) {
            throw new ValidationException("invalid cuisineTypes", Map.of("cuisineTypes", "must contain at least one value"));
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String cuisine : selected) {
            if (cuisine == null || cuisine.isBlank()) {
                continue;
            }
            String cleaned = cuisine.trim();
            if (!AppCuisineCatalog.isSupported(cleaned)) {
                throw new ValidationException("invalid cuisineTypes", Map.of("cuisineTypes", "contains unsupported value: " + cleaned));
            }
            String key = cleaned.toLowerCase(Locale.ROOT);
            normalized.add(Arrays.stream(key.split("\\s+"))
                    .map(token -> token.isBlank() ? token : Character.toUpperCase(token.charAt(0)) + token.substring(1))
                    .reduce((left, right) -> left + " " + right)
                    .orElse(cleaned));
        }

        if (normalized.isEmpty()) {
            throw new ValidationException("invalid cuisineTypes", Map.of("cuisineTypes", "must contain at least one non-blank value"));
        }
        return List.copyOf(normalized);
    }

    private RestaurantData toRestaurantData(Restaurant restaurant) {
        RestaurantData model = new RestaurantData();
        model.setId(restaurant.getId());
        model.setOwnerUserId(restaurant.getOwnerUserId());
        model.setName(restaurant.getName());
        model.setCuisineType(restaurant.getCuisineType());
        model.setCuisineTypes(restaurant.getCuisineTypes());
        model.setDescription(restaurant.getDescription());
        model.setImageUrl(restaurant.getImageUrl());
        model.setAddressLine(restaurant.getAddressLine());
        model.setLatitude(restaurant.getLatitude());
        model.setLongitude(restaurant.getLongitude());
        model.setH3IndexRes9(restaurant.getH3IndexRes9());
        model.setAvgRating(restaurant.getAvgRating());
        model.setReviewCount(restaurant.getReviewCount());
        model.setStatus(restaurant.getStatus());
        model.setOpen(restaurant.isOpen());
        model.setMaxDeliveryKm(restaurant.getMaxDeliveryKm());
        return model;
    }

    private MenuCategoryData toMenuCategoryData(MenuCategory category) {
        MenuCategoryData model = new MenuCategoryData();
        model.setId(category.getId());
        model.setRestaurantId(category.getRestaurantId());
        model.setName(category.getName());
        model.setSortOrder(category.getSortOrder());
        model.setActive(category.isActive());
        return model;
    }

    private MenuItemData toMenuItemData(MenuItem item) {
        MenuItemData model = new MenuItemData();
        model.setId(item.getId());
        model.setRestaurantId(item.getRestaurantId());
        model.setCategoryId(item.getCategoryId());
        model.setName(item.getName());
        model.setDescription(item.getDescription());
        model.setImageUrl(item.getImageUrl());
        model.setPrice(item.getPrice());
        model.setActive(item.isActive());
        model.setAvailable(item.isAvailable());
        model.setDeletedAt(item.getDeletedAt());
        return model;
    }
}
