package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.MerchantCatalogUseCase;
import com.foodya.backend.application.dto.CreateMenuCategoryRequest;
import com.foodya.backend.application.dto.CreateMenuItemRequest;
import com.foodya.backend.application.dto.MenuCategoryData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.CreateRestaurantRequest;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.UpdateMenuCategoryRequest;
import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import com.foodya.backend.application.dto.UpdateMenuItemRequest;
import com.foodya.backend.application.dto.UpdateRestaurantRequest;
import com.foodya.backend.application.constants.AppCuisineCatalog;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.MenuCategoryResponse;
import com.foodya.backend.interfaces.rest.dto.MenuItemResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.mapper.CommonApiMapper;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
@Tag(name = "Merchant Catalog", description = "Merchant restaurant, category, and menu item management")
@Validated
public class MerchantCatalogController {

    private final MerchantCatalogUseCase merchantCatalogService;

    public MerchantCatalogController(MerchantCatalogUseCase merchantCatalogService) {
        this.merchantCatalogService = merchantCatalogService;
    }

    @PostMapping(value = "/restaurants", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create restaurant")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> createRestaurant(Authentication authentication,
                                                                                          @Valid @RequestBody CreateRestaurantRequest request,
                                                                                          HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        RestaurantData restaurant = merchantCatalogService.createRestaurant(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiSuccessResponse.of(CommonApiMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping(value = "/restaurants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create restaurant with image")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> createRestaurantWithImage(Authentication authentication,
                                                                                                   @Valid @RequestPart("payload") CreateRestaurantRequest request,
                                                                                                   @RequestPart(value = "backgroundFile", required = false) MultipartFile backgroundFile,
                                                                                                   @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
                                                                                                   HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        RestaurantData restaurant = merchantCatalogService.createRestaurant(merchantId, request);
        restaurant = applyRestaurantImages(merchantId, restaurant.getId(), restaurant, backgroundFile, avatarFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(CommonApiMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping(value = "/restaurants/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update restaurant")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> updateRestaurant(Authentication authentication,
                                                                                          @PathVariable String id,
                                                                                          @Valid @RequestBody UpdateRestaurantRequest request,
                                                                                          HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        RestaurantData restaurant = merchantCatalogService.updateRestaurant(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping(value = "/restaurants/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update restaurant with image")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> updateRestaurantWithImage(Authentication authentication,
                                                                                                   @PathVariable String id,
                                                                                                   @Valid @RequestPart("payload") UpdateRestaurantRequest request,
                                                                                                   @RequestPart(value = "backgroundFile", required = false) MultipartFile backgroundFile,
                                                                                                   @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
                                                                                                   HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        UUID restaurantId = parseUuid(id, "id");
        RestaurantData restaurant = merchantCatalogService.updateRestaurant(merchantId, restaurantId, request);
        restaurant = applyRestaurantImages(merchantId, restaurantId, restaurant, backgroundFile, avatarFile);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toRestaurantDetailResponse(restaurant), RequestTrace.from(httpServletRequest)));
    }

    @GetMapping("/cuisine-types")
    @Operation(summary = "List app-level cuisine types")
    public ResponseEntity<ApiSuccessResponse<java.util.List<String>>> cuisineTypes(HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiSuccessResponse.of(AppCuisineCatalog.displayValues(), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/restaurants/{id}/menu-categories")
    @Operation(summary = "Create menu category")
    public ResponseEntity<ApiSuccessResponse<MenuCategoryResponse>> createCategory(Authentication authentication,
                                                                                    @PathVariable String id,
                                                                                    @Valid @RequestBody CreateMenuCategoryRequest request,
                                                                                    HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        MenuCategoryData category = merchantCatalogService.createCategory(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiSuccessResponse.of(CommonApiMapper.toMenuCategoryResponse(category), RequestTrace.from(httpServletRequest)));
    }

    @GetMapping("/restaurants/{id}/menu-categories")
    @Operation(summary = "List menu categories")
    public ResponseEntity<ApiSuccessResponse<java.util.List<MenuCategoryResponse>>> listCategories(Authentication authentication,
                                                                                                    @PathVariable String id,
                                                                                                    @RequestParam(required = false) @Min(0) Integer page,
                                                                                                    @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                                    HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        PaginatedResult<MenuCategoryData> result = merchantCatalogService.listCategories(merchantId, parseUuid(id, "id"), page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items().stream().map(CommonApiMapper::toMenuCategoryResponse).toList(),
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @PatchMapping("/menu-categories/{id}")
    @Operation(summary = "Update menu category")
    public ResponseEntity<ApiSuccessResponse<MenuCategoryResponse>> updateCategory(Authentication authentication,
                                                                                    @PathVariable String id,
                                                                                    @Valid @RequestBody UpdateMenuCategoryRequest request,
                                                                                    HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        MenuCategoryData category = merchantCatalogService.updateCategory(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toMenuCategoryResponse(category), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/menu-categories/{id}")
    @Operation(summary = "Delete menu category")
    public ResponseEntity<Void> deleteCategory(Authentication authentication,
                                               @PathVariable String id) {
        UUID merchantId = principal(authentication);
        merchantCatalogService.deleteCategory(merchantId, parseUuid(id, "id"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/restaurants/{id}/menu-items", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create menu item")
    public ResponseEntity<ApiSuccessResponse<MenuItemResponse>> createMenuItem(Authentication authentication,
                                                                                @PathVariable String id,
                                                                                @Valid @RequestBody CreateMenuItemRequest request,
                                                                                HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        MenuItemData menuItem = merchantCatalogService.createMenuItem(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiSuccessResponse.of(CommonApiMapper.toMenuItemResponse(menuItem), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping(value = "/restaurants/{id}/menu-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create menu item with image")
    public ResponseEntity<ApiSuccessResponse<MenuItemResponse>> createMenuItemWithImage(Authentication authentication,
                                                                                         @PathVariable String id,
                                                                                         @Valid @RequestPart("payload") CreateMenuItemRequest request,
                                                                                         @RequestPart("file") MultipartFile file,
                                                                                         HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        UUID restaurantId = parseUuid(id, "id");
        MenuItemData menuItem = merchantCatalogService.createMenuItem(merchantId, restaurantId, request);

        try {
            menuItem = merchantCatalogService.uploadMenuItemImage(
                    merchantId,
                    menuItem.getId(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
        } catch (IOException ex) {
            throw new ValidationException("invalid file", java.util.Map.of("file", "cannot be read"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(CommonApiMapper.toMenuItemResponse(menuItem), RequestTrace.from(httpServletRequest)));
    }

    @GetMapping("/restaurants/{id}/menu-items")
    @Operation(summary = "List menu items")
    public ResponseEntity<ApiSuccessResponse<java.util.List<MenuItemResponse>>> listMenuItems(Authentication authentication,
                                                                                               @PathVariable String id,
                                                                                               @RequestParam(required = false) @Min(0) Integer page,
                                                                                               @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                               HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        PaginatedResult<MenuItemData> result = merchantCatalogService.listMenuItems(merchantId, parseUuid(id, "id"), page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items().stream().map(CommonApiMapper::toMenuItemResponse).toList(),
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @PatchMapping("/menu-items/{id}")
    @Operation(summary = "Update menu item")
    public ResponseEntity<ApiSuccessResponse<MenuItemResponse>> updateMenuItem(Authentication authentication,
                                                                                @PathVariable String id,
                                                                                @Valid @RequestBody UpdateMenuItemRequest request,
                                                                                HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        MenuItemData menuItem = merchantCatalogService.updateMenuItem(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toMenuItemResponse(menuItem), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/menu-items/{id}")
    @Operation(summary = "Soft delete menu item")
    public ResponseEntity<Void> deleteMenuItem(Authentication authentication,
                                               @PathVariable String id) {
        UUID merchantId = principal(authentication);
        merchantCatalogService.softDeleteMenuItem(merchantId, parseUuid(id, "id"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/menu-items/{id}/availability")
    @Operation(summary = "Update menu item availability")
    public ResponseEntity<ApiSuccessResponse<MenuItemResponse>> updateAvailability(Authentication authentication,
                                                                                   @PathVariable String id,
                                                                                   @Valid @RequestBody UpdateMenuItemAvailabilityRequest request,
                                                                                   HttpServletRequest httpServletRequest) {
        UUID merchantId = principal(authentication);
        MenuItemData menuItem = merchantCatalogService.updateAvailability(merchantId, parseUuid(id, "id"), request);
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toMenuItemResponse(menuItem), RequestTrace.from(httpServletRequest)));
    }

    private static UUID principal(Authentication authentication) {
        return CurrentUser.userId(authentication);
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", java.util.Map.of(field, "must be a valid UUID"));
        }
    }

    private RestaurantData applyRestaurantImages(UUID merchantId,
                                                 UUID restaurantId,
                                                 RestaurantData restaurant,
                                                 MultipartFile backgroundFile,
                                                 MultipartFile avatarFile) {
        try {
            if (backgroundFile != null && !backgroundFile.isEmpty()) {
                restaurant = merchantCatalogService.uploadRestaurantBackgroundImage(
                        merchantId,
                        restaurantId,
                        backgroundFile.getOriginalFilename(),
                        backgroundFile.getContentType(),
                        backgroundFile.getBytes()
                );
            }
            if (avatarFile != null && !avatarFile.isEmpty()) {
                restaurant = merchantCatalogService.uploadRestaurantAvatarImage(
                        merchantId,
                        restaurantId,
                        avatarFile.getOriginalFilename(),
                        avatarFile.getContentType(),
                        avatarFile.getBytes()
                );
            }
            return restaurant;
        } catch (IOException ex) {
            throw new ValidationException("invalid file", java.util.Map.of("file", "cannot be read"));
        }
    }
}
