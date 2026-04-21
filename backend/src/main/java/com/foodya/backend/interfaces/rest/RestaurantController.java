package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.CatalogService;
import com.foodya.backend.interfaces.rest.dto.CategoryTaxonomyResponse;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantSearchView;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.MenuItemResponse;
import com.foodya.backend.interfaces.rest.dto.PageMetadata;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.mapper.CommonApiMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import com.foodya.backend.application.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Catalog", description = "Public restaurant and menu browsing")
@Validated
public class RestaurantController {

    private final CatalogService catalogService;

    public RestaurantController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Search restaurants", description = "Grouped restaurant results with matched menu items and optional nearby mode")
    public ResponseEntity<ApiSuccessResponse<java.util.List<RestaurantSearchView>>> search(@RequestParam(required = false) String q,
                                                                                                 @RequestParam(required = false) String cuisine,
                                                                                                 @RequestParam(required = false) BigDecimal minRating,
                                                                                                 @RequestParam(required = false) Boolean openNow,
                                                                                                 @RequestParam(required = false) java.util.List<String> taxonomyCodes,
                                                                                                 @RequestParam(required = false) @Min(0) Integer page,
                                                                                                 @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                                 @RequestParam(required = false, defaultValue = "relevance") String sort,
                                                                                                 @RequestParam(required = false) @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
                                                                                                 @RequestParam(required = false) @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lng,
                                                                                                 @RequestParam(required = false) @DecimalMin(value = "0.1") BigDecimal radiusKm,
                                                                                                 HttpServletRequest httpServletRequest) {
        PaginatedResult<RestaurantSearchView> result = catalogService.searchRestaurants(q, cuisine, minRating, openNow, taxonomyCodes, page, size, sort, lat, lng, radiusKm);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items(),
            new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Nearby restaurants", description = "Nearby list using H3 prefilter and Haversine distance ordering")
    public ResponseEntity<ApiSuccessResponse<java.util.List<RestaurantSearchView>>> nearby(@RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
                                                                                                 @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lng,
                                                                                                 @RequestParam @DecimalMin(value = "0.1") BigDecimal radiusKm,
                                                                                                 @RequestParam(required = false) @Min(0) Integer page,
                                                                                                 @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                                 HttpServletRequest httpServletRequest) {
        PaginatedResult<RestaurantSearchView> result = catalogService.nearby(lat, lng, radiusKm, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items(),
            new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Restaurant detail")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> detail(@PathVariable String id,
                                                                                HttpServletRequest httpServletRequest) {
        UUID restaurantId = parseUuid(id, "id");
        return ResponseEntity.ok(ApiSuccessResponse.of(
            CommonApiMapper.toRestaurantDetailResponse(catalogService.restaurantDetail(restaurantId)),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/{id}/menu-items")
    @Operation(summary = "Restaurant menu items")
    public ResponseEntity<ApiSuccessResponse<java.util.List<MenuItemResponse>>> menuItems(@PathVariable String id,
                                                                                            @RequestParam(required = false) String q,
                                                                                            @RequestParam(required = false) java.util.List<String> taxonomyCodes,
                                                                                            @RequestParam(required = false, defaultValue = "popularity_desc") String sort,
                                                                                            @RequestParam(required = false) @Min(0) Integer page,
                                                                                            @RequestParam(required = false) @Min(1) @Max(200) Integer size,
                                                                                            HttpServletRequest httpServletRequest) {
        UUID restaurantId = parseUuid(id, "id");
        PaginatedResult<MenuItemData> result = catalogService.publicMenuItems(restaurantId, q, taxonomyCodes, sort, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items().stream().map(CommonApiMapper::toMenuItemResponse).toList(),
                new PageMetadata(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/category-taxonomies")
    @Operation(summary = "List app-level category taxonomies")
    public ResponseEntity<ApiSuccessResponse<java.util.List<CategoryTaxonomyResponse>>> categoryTaxonomies(HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiSuccessResponse.of(
                catalogService.listCategoryTaxonomies().stream().map(CommonApiMapper::toCategoryTaxonomyResponse).toList(),
                RequestTrace.from(httpServletRequest)
        ));
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", java.util.Map.of(field, "must be a valid UUID"));
        }
    }
}
