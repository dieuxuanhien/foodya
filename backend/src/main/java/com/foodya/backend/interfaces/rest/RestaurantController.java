package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.usecases.CatalogService;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantSearchResponse;
import com.foodya.backend.application.dto.MenuItemModel;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.MenuItemResponse;
import com.foodya.backend.interfaces.rest.dto.PageMeta;
import com.foodya.backend.interfaces.rest.dto.RestaurantDetailResponse;
import com.foodya.backend.interfaces.rest.mapper.RestDtoMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import com.foodya.backend.application.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
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
public class RestaurantController {

    private final CatalogService catalogService;

    public RestaurantController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Search restaurants", description = "Grouped restaurant results with matched menu items and optional nearby mode")
    public ResponseEntity<ApiSuccessResponse<java.util.List<RestaurantSearchResponse>>> search(@RequestParam(required = false) String q,
                                                                                                 @RequestParam(required = false) String cuisine,
                                                                                                 @RequestParam(required = false) BigDecimal minRating,
                                                                                                 @RequestParam(required = false) Boolean openNow,
                                                                                                 @RequestParam(required = false) Integer page,
                                                                                                 @RequestParam(required = false) Integer size,
                                                                                                 @RequestParam(required = false, defaultValue = "relevance") String sort,
                                                                                                 @RequestParam(required = false) BigDecimal lat,
                                                                                                 @RequestParam(required = false) BigDecimal lng,
                                                                                                 @RequestParam(required = false) BigDecimal radiusKm,
                                                                                                 HttpServletRequest httpServletRequest) {
        PaginatedResult<RestaurantSearchResponse> result = catalogService.searchRestaurants(q, cuisine, minRating, openNow, page, size, sort, lat, lng, radiusKm);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items(),
            new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Nearby restaurants", description = "Nearby list using H3 prefilter and Haversine distance ordering")
    public ResponseEntity<ApiSuccessResponse<java.util.List<RestaurantSearchResponse>>> nearby(@RequestParam BigDecimal lat,
                                                                                                 @RequestParam BigDecimal lng,
                                                                                                 @RequestParam BigDecimal radiusKm,
                                                                                                 @RequestParam(required = false) Integer page,
                                                                                                 @RequestParam(required = false) Integer size,
                                                                                                 HttpServletRequest httpServletRequest) {
        PaginatedResult<RestaurantSearchResponse> result = catalogService.nearby(lat, lng, radiusKm, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items(),
            new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages()),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Restaurant detail")
    public ResponseEntity<ApiSuccessResponse<RestaurantDetailResponse>> detail(@PathVariable String id,
                                                                                HttpServletRequest httpServletRequest) {
        UUID restaurantId = parseUuid(id, "id");
        return ResponseEntity.ok(ApiSuccessResponse.of(
            RestDtoMapper.toRestaurantDetailResponse(catalogService.restaurantDetail(restaurantId)),
                RequestTrace.from(httpServletRequest)
        ));
    }

    @GetMapping("/{id}/menu-items")
    @Operation(summary = "Restaurant menu items")
    public ResponseEntity<ApiSuccessResponse<java.util.List<MenuItemResponse>>> menuItems(@PathVariable String id,
                                                                                            @RequestParam(required = false) String q,
                                                                                            @RequestParam(required = false) String categoryId,
                                                                                            @RequestParam(required = false, defaultValue = "popularity_desc") String sort,
                                                                                            @RequestParam(required = false) Integer page,
                                                                                            @RequestParam(required = false) Integer size,
                                                                                            HttpServletRequest httpServletRequest) {
        UUID restaurantId = parseUuid(id, "id");
        PaginatedResult<MenuItemModel> result = catalogService.publicMenuItems(restaurantId, q, categoryId, sort, page, size);
        return ResponseEntity.ok(ApiSuccessResponse.of(
            result.items().stream().map(RestDtoMapper::toMenuItemResponse).toList(),
                new PageMeta(result.page(), result.size(), result.totalElements(), result.totalPages()),
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
