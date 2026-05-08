import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/location/geolocation_service.dart';
import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../cubit/customer_home_cubit.dart';
import '../cubit/customer_home_state.dart';

enum _CustomerSessionAction { refresh, logoutAll }

class CustomerHomePage extends StatelessWidget {
  const CustomerHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => CustomerHomeCubit(
            catalogRepository: context.read<CustomerCatalogRepository>(),
            geolocationService: context.read<GeolocationService>(),
          )..initialize(),
      child: const _CustomerHomeView(),
    );
  }
}

class _CustomerHomeView extends StatelessWidget {
  const _CustomerHomeView();

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<CustomerHomeCubit, CustomerHomeState>(
      listenWhen:
          (previous, current) =>
              previous.locationMessage != current.locationMessage ||
              previous.nearbyMessage != current.nearbyMessage,
      listener: (context, state) {
        final snackbarMessage = state.locationMessage ?? state.nearbyMessage;
        if (snackbarMessage == null) {
          return;
        }

        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(snackbarMessage)));
      },
      builder: (context, homeState) {
        return Scaffold(
          appBar: AppBar(
            title: Row(
              children: [
                IconButton(
                  onPressed:
                      homeState.isRefreshingLocation
                          ? null
                          : () =>
                              context
                                  .read<CustomerHomeCubit>()
                                  .refreshLocation(),
                  icon:
                      homeState.isRefreshingLocation
                          ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                          : const Icon(Icons.gps_fixed),
                  tooltip: 'Update current location',
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    homeState.locationLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
            actions: [
              PopupMenuButton<_CustomerSessionAction>(
                onSelected: (action) async {
                  final cubit = context.read<LoginCubit>();
                  switch (action) {
                    case _CustomerSessionAction.refresh:
                      await cubit.refreshToken();
                      break;
                    case _CustomerSessionAction.logoutAll:
                      await cubit.logoutAll();
                      break;
                  }

                  final state = cubit.state;
                  final message = state.errorMessage ?? state.infoMessage;
                  if (message != null && context.mounted) {
                    ScaffoldMessenger.of(
                      context,
                    ).showSnackBar(SnackBar(content: Text(message)));
                    cubit.clearFeedback();
                  }
                },
                itemBuilder:
                    (context) => const [
                      PopupMenuItem(
                        value: _CustomerSessionAction.refresh,
                        child: Text('Refresh Token'),
                      ),
                      PopupMenuItem(
                        value: _CustomerSessionAction.logoutAll,
                        child: Text('Logout All Sessions'),
                      ),
                    ],
                icon: const Icon(Icons.more_vert),
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const Text(
                'Nearby Restaurants',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 10),
              _NearbyRestaurantsRow(state: homeState),
              const SizedBox(height: 16),
              const _FeatureCard(
                title: 'Discover Restaurants',
                subtitle: 'SRS FR07, FR08, FR09',
                icon: Icons.search,
                routePath: '/customer/restaurants',
              ),
              const SizedBox(height: 12),
              const _FeatureCard(
                title: 'Manage Cart and Checkout',
                subtitle: 'SRS FR10, FR27',
                icon: Icons.shopping_bag_outlined,
              ),
              const SizedBox(height: 12),
              const _FeatureCard(
                title: 'Track Orders and Reviews',
                subtitle: 'SRS FR11, FR12',
                icon: Icons.delivery_dining_outlined,
              ),
            ],
          ),
        );
      },
    );
  }
}

class _NearbyRestaurantsRow extends StatelessWidget {
  const _NearbyRestaurantsRow({required this.state});

  final CustomerHomeState state;

  @override
  Widget build(BuildContext context) {
    if (state.isNearbyLoading) {
      return const SizedBox(
        height: 150,
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (state.nearbyRestaurants.isEmpty) {
      return Container(
        height: 120,
        padding: const EdgeInsets.all(12),
        alignment: Alignment.centerLeft,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(
          state.nearbyMessage ?? 'No nearby restaurants found.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      );
    }

    return SizedBox(
      height: 190,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: state.nearbyRestaurants.length,
        separatorBuilder: (_, _) => const SizedBox(width: 10),
        itemBuilder:
            (context, index) => _NearbyRestaurantCard(
              restaurant: state.nearbyRestaurants[index],
            ),
      ),
    );
  }
}

class _NearbyRestaurantCard extends StatelessWidget {
  const _NearbyRestaurantCard({required this.restaurant});

  final RestaurantSearchItem restaurant;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 240,
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap:
              () => context.push(
                '/customer/restaurants/${restaurant.restaurantId}',
              ),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  restaurant.restaurantName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text(
                  restaurant.cuisine,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const Spacer(),
                Wrap(
                  spacing: 8,
                  runSpacing: 6,
                  children: [
                    Chip(
                      visualDensity: VisualDensity.compact,
                      label: Text('⭐ ${restaurant.rating.toStringAsFixed(1)}'),
                    ),
                    if (restaurant.distanceKm != null)
                      Chip(
                        visualDensity: VisualDensity.compact,
                        label: Text(
                          '${restaurant.distanceKm!.toStringAsFixed(1)} km',
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    this.routePath,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final String? routePath;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        leading: Icon(icon),
        title: Text(title),
        subtitle: Text(subtitle),
        trailing:
            routePath == null
                ? null
                : const Icon(Icons.arrow_forward_ios_rounded, size: 16),
        onTap: routePath == null ? null : () => context.push(routePath!),
      ),
    );
  }
}
