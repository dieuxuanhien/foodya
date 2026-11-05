package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CategoryTaxonomyData;
import com.foodya.backend.application.dto.MenuItemData;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantData;
import com.foodya.backend.application.dto.RestaurantSearchView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogUseCase {

    List<CategoryTaxonomyData> listCategoryTaxonomies();

    PaginatedResult<RestaurantSearchView> searchRestaurants(String q,
                                                           String cuisine,
                                                           BigDecimal minRating,
                                                           Boolean openNow,
                                                           List<String> taxonomyCodes,
                                                           Integer page,
                                                           Integer size,
                                                           String sort,
                                                           BigDecimal lat,
                                                           BigDecimal lng,
                                                           BigDecimal radiusKm);

    PaginatedResult<RestaurantSearchView> nearby(BigDecimal lat,
                                                BigDecimal lng,
                                                BigDecimal radiusKm,
                                                Integer page,
                                                Integer size);

    RestaurantData restaurantDetail(UUID restaurantId);

    PaginatedResult<MenuItemData> publicMenuItems(UUID restaurantId,
                                                 String keyword,
                                                 List<String> taxonomyCodes,
                                                 String sort,
                                                 Integer page,
                                                 Integer size);
}
