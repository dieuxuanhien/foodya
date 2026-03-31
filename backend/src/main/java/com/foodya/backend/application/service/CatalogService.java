package com.foodya.backend.application.service;

import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.SystemParameter;
import com.foodya.backend.application.dto.MatchedMenuItemResponse;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.RestaurantSearchResponse;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.application.port.out.SystemParameterPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final List<RestaurantStatus> PUBLIC_STATUSES = List.of(RestaurantStatus.ACTIVE);

    private final RestaurantPort restaurantPort;
    private final MenuItemPort menuItemPort;
    private final SystemParameterPort systemParameterPort;
    private final PaginationPolicyService paginationPolicyService;
    private final GeoService geoService;

    public CatalogService(RestaurantPort restaurantPort,
                          MenuItemPort menuItemPort,
                          SystemParameterPort systemParameterPort,
                          PaginationPolicyService paginationPolicyService,
                          GeoService geoService) {
        this.restaurantPort = restaurantPort;
        this.menuItemPort = menuItemPort;
        this.systemParameterPort = systemParameterPort;
        this.paginationPolicyService = paginationPolicyService;
        this.geoService = geoService;
    }

    public PaginatedResult<RestaurantSearchResponse> searchRestaurants(String q,
                                                                       String cuisine,
                                                                       BigDecimal minRating,
                                                                       Boolean openNow,
                                                                       Integer page,
                                                                       Integer size,
                                                                       String sort,
                                                                       BigDecimal lat,
                                                                       BigDecimal lng,
                                                                       BigDecimal radiusKm) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        String keyword = q == null ? "" : q.trim();

        Map<UUID, List<MenuItem>> matchedItemsByRestaurant = matchedItemsByRestaurant(keyword);
        List<Restaurant> candidates = filterByKeyword(keyword, matchedItemsByRestaurant.keySet());
        candidates = applyRestaurantFilters(candidates, cuisine, minRating, openNow);

        Map<UUID, BigDecimal> distanceByRestaurant = Map.of();
        if (lat != null || lng != null || radiusKm != null) {
            if (lat == null || lng == null) {
                throw new ValidationException("invalid nearby parameters", Map.of("lat/lng", "both lat and lng are required"));
            }
            BigDecimal resolvedRadius = radiusKm == null ? BigDecimal.valueOf(maxNearbyRadius()) : radiusKm;
            candidates = applyNearbyFilter(candidates, lat.doubleValue(), lng.doubleValue(), resolvedRadius.doubleValue());
            distanceByRestaurant = computeDistanceMap(candidates, lat.doubleValue(), lng.doubleValue());
        }

        final Map<UUID, BigDecimal> resolvedDistanceByRestaurant = distanceByRestaurant;
        List<Restaurant> sorted = sortRestaurants(candidates, sort, keyword, resolvedDistanceByRestaurant);
        int from = spec.offset();
        int to = Math.min(sorted.size(), from + spec.size());
        List<Restaurant> pageContent = from >= sorted.size() ? List.of() : sorted.subList(from, to);

        List<RestaurantSearchResponse> responses = pageContent.stream()
                .map(restaurant -> RestaurantSearchResponse.from(
                        restaurant,
                        resolvedDistanceByRestaurant.get(restaurant.getId()),
                        matchedItemsByRestaurant.getOrDefault(restaurant.getId(), List.of())
                                .stream()
                                .map(MatchedMenuItemResponse::from)
                                .toList()
                ))
                .toList();

        int totalPages = (int) Math.ceil((double) sorted.size() / spec.size());
        return new PaginatedResult<>(responses, spec.page(), spec.size(), sorted.size(), totalPages);
    }

    public PaginatedResult<RestaurantSearchResponse> nearby(BigDecimal lat,
                                                            BigDecimal lng,
                                                            BigDecimal radiusKm,
                                                            Integer page,
                                                            Integer size) {
        if (lat == null || lng == null || radiusKm == null) {
            throw new ValidationException("invalid nearby parameters", Map.of("lat/lng/radiusKm", "must be provided"));
        }

        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        Set<String> ring = h3Ring(lat.doubleValue(), lng.doubleValue(), radiusKm.doubleValue());
        List<Restaurant> prefiltered = restaurantPort.findByH3IndexRes9InAndStatusIn(ring, PUBLIC_STATUSES);
        List<Restaurant> candidates = applyNearbyFilter(prefiltered, lat.doubleValue(), lng.doubleValue(), radiusKm.doubleValue());
        Map<UUID, BigDecimal> distanceByRestaurant = computeDistanceMap(candidates, lat.doubleValue(), lng.doubleValue());

        List<Restaurant> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparing(r -> distanceByRestaurant.get(r.getId())));

        int from = spec.offset();
        int to = Math.min(sorted.size(), from + spec.size());
        List<Restaurant> pageContent = from >= sorted.size() ? List.of() : sorted.subList(from, to);

        List<RestaurantSearchResponse> responses = pageContent.stream()
                .map(restaurant -> RestaurantSearchResponse.from(restaurant, distanceByRestaurant.get(restaurant.getId()), List.of()))
                .toList();

        int totalPages = (int) Math.ceil((double) sorted.size() / spec.size());
        return new PaginatedResult<>(responses, spec.page(), spec.size(), sorted.size(), totalPages);
    }

    public Restaurant restaurantDetail(UUID restaurantId) {
        return restaurantPort.findByIdAndStatusIn(restaurantId, PUBLIC_STATUSES)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));
    }

    public PaginatedResult<MenuItem> publicMenuItems(UUID restaurantId,
                                                     String keyword,
                                                     String categoryId,
                                                     String sort,
                                                     Integer page,
                                                     Integer size) {
        PaginationPolicyService.PaginationSpec spec = paginationPolicyService.page(page, size);
        restaurantDetail(restaurantId);

        List<MenuItem> filtered = menuItemPort.findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(restaurantId)
                .stream()
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesCategory(item, categoryId))
                .collect(Collectors.toCollection(ArrayList::new));

        sortMenuItems(filtered, sort);
        int from = spec.offset();
        int to = Math.min(filtered.size(), from + spec.size());
        List<MenuItem> pageContent = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        int totalPages = (int) Math.ceil((double) filtered.size() / spec.size());
        return new PaginatedResult<>(pageContent, spec.page(), spec.size(), filtered.size(), totalPages);
    }

    private List<Restaurant> applyRestaurantFilters(List<Restaurant> restaurants,
                                                    String cuisine,
                                                    BigDecimal minRating,
                                                    Boolean openNow) {
        return restaurants.stream()
                .filter(restaurant -> cuisine == null || cuisine.isBlank() || restaurant.getCuisineType().equalsIgnoreCase(cuisine.trim()))
                .filter(restaurant -> minRating == null || restaurant.getAvgRating().compareTo(minRating) >= 0)
                .filter(restaurant -> openNow == null || restaurant.isOpen() == openNow)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Restaurant> filterByKeyword(String keyword, Collection<UUID> itemMatchRestaurantIds) {
        List<Restaurant> active = restaurantPort.findAll().stream()
                .filter(restaurant -> PUBLIC_STATUSES.contains(restaurant.getStatus()))
                .toList();
        if (keyword.isBlank()) {
            return new ArrayList<>(active);
        }

        String lower = keyword.toLowerCase();
        return active.stream()
                .filter(restaurant -> restaurant.getName().toLowerCase().contains(lower) || itemMatchRestaurantIds.contains(restaurant.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<UUID, List<MenuItem>> matchedItemsByRestaurant(String keyword) {
        if (keyword.isBlank()) {
            return Map.of();
        }

        return menuItemPort.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(keyword)
                .stream()
                .collect(Collectors.groupingBy(MenuItem::getRestaurantId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<Restaurant> applyNearbyFilter(List<Restaurant> restaurants,
                                               double lat,
                                               double lng,
                                               double radiusKm) {
        if (radiusKm <= 0 || radiusKm > maxNearbyRadius()) {
            throw new ValidationException("invalid nearby radius", Map.of("radiusKm", "must be > 0 and <= search.nearby.max_radius_km"));
        }

        Set<String> ring = h3Ring(lat, lng, radiusKm);
        List<Restaurant> h3Candidates = restaurants.stream()
                .filter(restaurant -> ring.contains(restaurant.getH3IndexRes9()))
                .toList();

        return h3Candidates.stream()
                .filter(restaurant -> geoService
                        .haversineKm(lat, lng, restaurant.getLatitude().doubleValue(), restaurant.getLongitude().doubleValue())
                        .compareTo(BigDecimal.valueOf(radiusKm)) <= 0)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<String> h3Ring(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0 || radiusKm > maxNearbyRadius()) {
            throw new ValidationException("invalid nearby radius", Map.of("radiusKm", "must be > 0 and <= search.nearby.max_radius_km"));
        }
        return geoService.h3KRingRes9(lat, lng, radiusKm);
    }

    private static boolean matchesKeyword(MenuItem item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return item.getName().toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private static boolean matchesCategory(MenuItem item, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return true;
        }
        try {
            return item.getCategoryId().equals(UUID.fromString(categoryId));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid categoryId", Map.of("categoryId", "must be a valid UUID"));
        }
    }

    private static void sortMenuItems(List<MenuItem> items, String sort) {
        String resolvedSort = sort == null ? "popularity_desc" : sort.trim().toLowerCase();
        if ("name_asc".equals(resolvedSort)) {
            items.sort(Comparator.comparing(MenuItem::getName));
            return;
        }
        if ("price_asc".equals(resolvedSort)) {
            items.sort(Comparator.comparing(MenuItem::getPrice));
            return;
        }
        if ("price_desc".equals(resolvedSort)) {
            items.sort(Comparator.comparing(MenuItem::getPrice).reversed());
            return;
        }
        if (!"popularity_desc".equals(resolvedSort)) {
            throw new ValidationException("invalid sort", Map.of("sort", "must be one of popularity_desc,name_asc,price_asc,price_desc"));
        }

        // Phase 2 popularity proxy until order analytics exists in later phases.
        items.sort(Comparator.comparing(MenuItem::getName));
    }

    private Map<UUID, BigDecimal> computeDistanceMap(List<Restaurant> restaurants, double lat, double lng) {
        Map<UUID, BigDecimal> distances = new LinkedHashMap<>();
        restaurants.forEach(restaurant -> distances.put(
                restaurant.getId(),
                geoService.haversineKm(lat, lng, restaurant.getLatitude().doubleValue(), restaurant.getLongitude().doubleValue())
        ));
        return distances;
    }

    private List<Restaurant> sortRestaurants(List<Restaurant> restaurants,
                                             String sort,
                                             String keyword,
                                             Map<UUID, BigDecimal> distanceByRestaurant) {
        List<Restaurant> sorted = new ArrayList<>(restaurants);
        String resolvedSort = sort == null ? "relevance" : sort.trim().toLowerCase();

        if ("rating_desc".equals(resolvedSort)) {
            sorted.sort(Comparator.comparing(Restaurant::getAvgRating).reversed().thenComparing(Restaurant::getName));
            return sorted;
        }

        if ("distance_asc".equals(resolvedSort)) {
            if (distanceByRestaurant.isEmpty()) {
                throw new ValidationException("invalid sort", Map.of("sort", "distance_asc requires lat/lng"));
            }
            sorted.sort(Comparator.comparing(r -> distanceByRestaurant.get(r.getId())));
            return sorted;
        }

        if (!"relevance".equals(resolvedSort)) {
            throw new ValidationException("invalid sort", Map.of("sort", "must be one of relevance,rating_desc,distance_asc"));
        }

        if (keyword.isBlank()) {
            sorted.sort(Comparator.comparing(Restaurant::getName));
            return sorted;
        }

        String lower = keyword.toLowerCase();
        sorted.sort(Comparator
                .comparing((Restaurant restaurant) -> !restaurant.getName().toLowerCase().startsWith(lower))
                .thenComparing(Restaurant::getName));
        return sorted;
    }

    private double maxNearbyRadius() {
        return systemParameterPort.findById("search.nearby.max_radius_km")
                .map(SystemParameter::getValue)
                .map(Double::parseDouble)
                .orElse(10.0);
    }
}
