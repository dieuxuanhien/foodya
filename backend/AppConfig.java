
	@Bean
	public RouteCalculationPort goongTripAdapter(GoongMapsClient goongMapsClient, ObjectMapper objectMapper) {
		return new com.foodya.backend.infrastructure.adapter.integration.GoongTripAdapter(goongMapsClient, objectMapper);
	}

	@Bean
	public DeliveryRoutePort deliveryRoutePort(
			com.foodya.backend.infrastructure.repository.DeliveryRouteRepository repository,
			ObjectMapper objectMapper) {
		return new com.foodya.backend.infrastructure.adapter.DeliveryRoutePersistenceAdapter(repository, objectMapper);
	}

	@Bean
	public DeliveryRouteUseCase deliveryRouteService(
			RouteCalculationPort routeCalculationPort,
			DeliveryRoutePort deliveryRoutePort,
			OrderManagementPort orderManagementPort,
			RestaurantPort restaurantPort,
			DriverOnlineSessionPort driverOnlineSessionPort,
			OrderBatchPort orderBatchPort) {
		return new com.foodya.backend.application.usecases.DeliveryRouteService(
				routeCalculationPort, deliveryRoutePort, orderManagementPort,
				restaurantPort, driverOnlineSessionPort, orderBatchPort);
	}
}
