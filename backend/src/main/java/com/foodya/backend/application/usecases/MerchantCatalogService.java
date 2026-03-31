package com.foodya.backend.application.usecases;

import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.application.dto.MenuCategoryModel;
import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.application.dto.RestaurantModel;
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
import com.foodya.backend.application.ports.out.MenuCategoryPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
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
    private final GeoPort geoPort;

    public MerchantCatalogService(RestaurantPort restaurantPort,
                                  MenuCategoryPort menuCategoryPort,
                                  MenuItemPort menuItemPort,
                                  SystemParameterPort systemParameterPort,
                                  PaginationPolicyService paginationPolicyService,
                                  GeoPort geoPort) {
        this.restaurantPort = restaurantPort;
        this.menuCategoryPort = menuCategoryPort;
        this.menuItemPort = menuItemPort;
        this.systemParameterPort = systemParameterPort;
        this.paginationPolicyService = paginationPolicyService;
        this.geoPort = geoPort;
    }

    @Transactional
    public RestaurantModel createRestaurant(UUID merchantUserId, CreateRestaurantRequest request) {
        validateRestaurantDistance(request.maxDeliveryKm());

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(merchantUserId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineType(request.cuisineType().trim());
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoPort.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return toRestaurantModel(restaurantPort.save(restaurant));
    }

    @Transactional
    public RestaurantModel updateRestaurant(UUID merchantUserId, UUID restaurantId, UpdateRestaurantRequest request) {
        validateRestaurantDistance(request.maxDeliveryKm());
        Restaurant restaurant = ownedRestaurant(merchantUserId, restaurantId);
        restaurant.setName(request.name().trim());
        restaurant.setCuisineType(request.cuisineType().trim());
        restaurant.setDescription(request.description());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3IndexRes9(geoPort.h3Res9(request.latitude().doubleValue(), request.longitude().doubleValue()));
        restaurant.setOpen(request.isOpen());
        restaurant.setMaxDeliveryKm(request.maxDeliveryKm());
        return toRestaurantModel(restaurantPort.save(restaurant));
    }

    @Transactional
    public MenuCategoryModel createCategory(UUID merchantUserId, UUID restaurantId, CreateMenuCategoryRequest request) {
        ownedRestaurant(merchantUserId, restaurantId);
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCase(restaurantId, request.name().trim())) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        MenuCategory category = new MenuCategory();
        category.setRestaurantId(restaurantId);
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return toMenuCategoryModel(menuCategoryPort.save(category));
    }

    @Transactional(readOnly = true)
    public PaginatedResult<MenuCategoryModel> listCategories(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        PaginatedResult<MenuCategory> result = menuCategoryPort.findByRestaurantIdAndActiveTrue(restaurantId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuCategoryModel).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Transactional
    public MenuCategoryModel updateCategory(UUID merchantUserId, UUID categoryId, UpdateMenuCategoryRequest request) {
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        if (menuCategoryPort.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(category.getRestaurantId(), request.name().trim(), categoryId)) {
            throw new ValidationException("category already exists", Map.of("name", "duplicate category name"));
        }

        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
        return toMenuCategoryModel(menuCategoryPort.save(category));
    }

    @Transactional
    public void deleteCategory(UUID merchantUserId, UUID categoryId) {
        MenuCategory category = menuCategoryPort.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("menu category not found"));
        ownedRestaurant(merchantUserId, category.getRestaurantId());
        menuCategoryPort.delete(category);
    }

    @Transactional
    public MenuItemModel createMenuItem(UUID merchantUserId, UUID restaurantId, CreateMenuItemRequest request) {
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
        return toMenuItemModel(menuItemPort.save(item));
    }

    @Transactional(readOnly = true)
    public PaginatedResult<MenuItemModel> listMenuItems(UUID merchantUserId, UUID restaurantId, Integer page, Integer size) {
        ownedRestaurant(merchantUserId, restaurantId);
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        PaginatedResult<MenuItem> result = menuItemPort.findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(restaurantId, spec.page(), spec.size());
        return new PaginatedResult<>(
                result.items().stream().map(this::toMenuItemModel).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Transactional
    public MenuItemModel updateMenuItem(UUID merchantUserId, UUID menuItemId, UpdateMenuItemRequest request) {
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
        return toMenuItemModel(menuItemPort.save(item));
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
    public MenuItemModel updateAvailability(UUID merchantUserId, UUID menuItemId, UpdateMenuItemAvailabilityRequest request) {
        MenuItem item = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        ownedRestaurant(merchantUserId, item.getRestaurantId());
        item.setAvailable(request.isAvailable());
        return toMenuItemModel(menuItemPort.save(item));
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

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }

    private RestaurantModel toRestaurantModel(Restaurant restaurant) {
        RestaurantModel model = new RestaurantModel();
        model.setId(restaurant.getId());
        model.setOwnerUserId(restaurant.getOwnerUserId());
        model.setName(restaurant.getName());
        model.setCuisineType(restaurant.getCuisineType());
        model.setDescription(restaurant.getDescription());
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

    private MenuCategoryModel toMenuCategoryModel(MenuCategory category) {
        MenuCategoryModel model = new MenuCategoryModel();
        model.setId(category.getId());
        model.setRestaurantId(category.getRestaurantId());
        model.setName(category.getName());
        model.setSortOrder(category.getSortOrder());
        model.setActive(category.isActive());
        return model;
    }

    private MenuItemModel toMenuItemModel(MenuItem item) {
        MenuItemModel model = new MenuItemModel();
        model.setId(item.getId());
        model.setRestaurantId(item.getRestaurantId());
        model.setCategoryId(item.getCategoryId());
        model.setName(item.getName());
        model.setDescription(item.getDescription());
        model.setPrice(item.getPrice());
        model.setActive(item.isActive());
        model.setAvailable(item.isAvailable());
        model.setDeletedAt(item.getDeletedAt());
        return model;
    }
}
