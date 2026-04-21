package com.foodya.backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.support.PaginationPolicy;
import com.foodya.backend.application.usecases.*;
import com.foodya.backend.application.usecases.policy.BackupPolicyEnforcementService;
import com.foodya.backend.application.usecases.policy.RetentionPolicyEnforcementService;
import com.foodya.backend.application.ports.out.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application Service Bean Configuration.
 * Keeps application use cases framework-free and wires them explicitly here.
 */
@Configuration
@EnableConfigurationProperties({
	SecretSourcesProperties.class,
	SecurityProperties.class,
	RateLimitProperties.class
})
public class AppConfig {

	@Bean
	public AuthService authService(
			UserAccountPort userAccountPort,
			RefreshTokenPort refreshTokenPort,
			PasswordResetChallengePort passwordResetChallengePort,
			AuditLogService auditLogService,
			PasswordHashPort passwordHashPort,
			TokenPort tokenPort,
			SecurityPolicyPort securityPolicyPort,
			OtpDeliveryPort otpDeliveryPort) {
		return new AuthService(userAccountPort, refreshTokenPort, passwordResetChallengePort,
				auditLogService, passwordHashPort, tokenPort, securityPolicyPort, otpDeliveryPort);
	}

	@Bean
	public ProfileService profileService(
			UserAccountPort userAccountPort,
			PasswordHashPort passwordHashPort,
			AuditLogService auditLogService) {
		return new ProfileService(userAccountPort, passwordHashPort, auditLogService);
	}

	@Bean
	public CatalogService catalogService(
			CatalogQueryPort catalogQueryPort,
			CategoryTaxonomyPort categoryTaxonomyPort,
			SystemParameterPort systemParameterPort,
			PaginationPolicy paginationPolicy,
			GeoPort geoPort) {
		return new CatalogService(catalogQueryPort, categoryTaxonomyPort, systemParameterPort, paginationPolicy, geoPort);
	}

	@Bean
	public MerchantCatalogService merchantCatalogService(
			RestaurantPort restaurantPort,
			MenuCategoryPort menuCategoryPort,
			MenuItemPort menuItemPort,
			CategoryTaxonomyPort categoryTaxonomyPort,
			SystemParameterPort systemParameterPort,
			PaginationPolicy paginationPolicy,
			GeoPort geoPort,
			MenuItemImageStoragePort menuItemImageStoragePort,
			RestaurantImageStoragePort restaurantImageStoragePort) {
		return new MerchantCatalogService(
				restaurantPort,
				menuCategoryPort,
				menuItemPort,
				categoryTaxonomyPort,
				systemParameterPort,
				paginationPolicy,
				geoPort,
				menuItemImageStoragePort,
				restaurantImageStoragePort
		);
	}

	@Bean
	public OrderCheckoutService orderCheckoutService(
			OrderCheckoutPort orderCheckoutPort,
			CartPort cartPort,
			CartItemPort cartItemPort,
			RouteDistancePort routeDistancePort,
			SystemParameterPort systemParameterPort,
			OrderEventPublisherPort orderEventPublisherPort) {
		return new OrderCheckoutService(orderCheckoutPort, cartPort, cartItemPort, routeDistancePort, systemParameterPort, orderEventPublisherPort);
	}

	@Bean
	public OrderLifecycleService orderLifecycleService(
			OrderManagementPort orderManagementPort,
			RestaurantPort restaurantPort,
			DeliveryTrackingPointPort deliveryTrackingPointPort,
			OrderEventPublisherPort orderEventPublisherPort) {
		return new OrderLifecycleService(orderManagementPort, restaurantPort, deliveryTrackingPointPort, orderEventPublisherPort);
	}

	@Bean
	public OrderReviewService orderReviewService(
			OrderManagementPort orderManagementPort,
			OrderReviewPort orderReviewPort,
			RestaurantPort restaurantPort) {
		return new OrderReviewService(orderManagementPort, orderReviewPort, restaurantPort);
	}

	@Bean
	public CartService cartService(
			CartPort cartPort,
			CartItemPort cartItemPort,
			MenuItemPort menuItemPort) {
		return new CartService(cartPort, cartItemPort, menuItemPort);
	}

	@Bean
	public NotificationService notificationService(
			NotificationLogPort notificationLogPort,
			PushNotificationPort pushNotificationPort,
			PaginationPolicy paginationPolicy) {
		return new NotificationService(notificationLogPort, pushNotificationPort, paginationPolicy);
	}

	@Bean
	public SystemParameterService systemParameterService(
			SystemParameterPort systemParameterPort,
			AuditLogPort auditLogPort) {
		return new SystemParameterService(systemParameterPort, auditLogPort);
	}

	@Bean
	public AdminUserService adminUserService(
			AdminUserPort adminUserPort,
			PaginationPolicy paginationPolicy,
			AuditLogService auditLogService) {
		return new AdminUserService(adminUserPort, paginationPolicy, auditLogService);
	}

	@Bean
	public AdminGovernanceService adminGovernanceService(
			AdminRestaurantPort adminRestaurantPort,
			AdminOrderPort adminOrderPort,
			AdminMenuItemPort adminMenuItemPort,
			PaginationPolicy paginationPolicy,
			AuditLogService auditLogService) {
		return new AdminGovernanceService(adminRestaurantPort, adminOrderPort, adminMenuItemPort, paginationPolicy, auditLogService);
	}

	@Bean
	public AdminCategoryTaxonomyService adminCategoryTaxonomyService(
			CategoryTaxonomyPort categoryTaxonomyPort,
			AuditLogService auditLogService) {
		return new AdminCategoryTaxonomyService(categoryTaxonomyPort, auditLogService);
	}

	@Bean
	public RevenueReportService revenueReportService(RevenueReportPort revenueReportPort) {
		return new RevenueReportService(revenueReportPort);
	}

	@Bean
	public AiRecommendationService aiRecommendationService(
			AiEmbeddingPort aiEmbeddingPort,
			AiCatalogVectorPort aiCatalogVectorPort,
			MenuItemPort menuItemPort,
			RestaurantPort restaurantPort,
			UserAccountPort userAccountPort,
			AiDraftPort aiDraftPort,
			WeatherContextPort weatherContextPort,
			AiChatHistoryPort aiChatHistoryPort,
			SystemParameterPort systemParameterPort,
			GeoPort geoPort,
			ObjectMapper objectMapper) {
		return new AiRecommendationService(aiEmbeddingPort, aiCatalogVectorPort, menuItemPort, restaurantPort, userAccountPort, aiDraftPort, weatherContextPort, aiChatHistoryPort, systemParameterPort, geoPort, objectMapper);
	}

	@Bean
	public BackupPolicyEnforcementService backupPolicyEnforcementService(SystemParameterPort systemParameterPort) {
		return new BackupPolicyEnforcementService(systemParameterPort);
	}

	@Bean
	public RetentionPolicyEnforcementService retentionPolicyEnforcementService(
			SystemParameterPort systemParameterPort,
			AuditLogPort auditLogPort,
			DeliveryTrackingPointPort deliveryTrackingPointPort,
			AiChatHistoryPort aiChatHistoryPort) {
		return new RetentionPolicyEnforcementService(systemParameterPort, auditLogPort, deliveryTrackingPointPort, aiChatHistoryPort);
	}

	@Bean
	public AuditLogService auditLogService(AuditLogPort auditLogPort) {
		return new AuditLogService(auditLogPort);
	}

	@Bean
	public PaginationPolicy paginationPolicy(SystemParameterPort systemParameterPort) {
		return new PaginationPolicy(systemParameterPort);
	}

	@Bean
	public HealthReadinessService healthReadinessService(HealthCheckPort healthCheckPort) {
		return new HealthReadinessService(healthCheckPort);
	}

	@Bean
	public IntegrationProbeService integrationProbeService(FirebaseConfigPort firebaseConfigPort,
			SupabaseConfigPort supabaseConfigPort) {
		return new IntegrationProbeService(firebaseConfigPort, supabaseConfigPort);
	}

	@Bean
	public IntegrationStatusService integrationStatusService(IntegrationSecretPort integrationSecretPort) {
		return new IntegrationStatusService(integrationSecretPort);
	}
}
