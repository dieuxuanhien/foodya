import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/location/geolocation_service.dart';
import '../../../../core/ui/foodya_ui.dart';
import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../cubit/customer_home_cubit.dart';
import '../cubit/customer_home_state.dart';
import '../widgets/manual_location_sheet.dart';

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
                          : () => _chooseLocationSource(context, homeState),
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
              _HomeHero(
                locationLabel: homeState.locationLabel,
                isRefreshingLocation: homeState.isRefreshingLocation,
                onLocationTap: () => _chooseLocationSource(context, homeState),
              ),
              const SizedBox(height: 20),
              FoodyaSectionHeader(
                title: 'Nearby restaurants',
                subtitle: 'Fresh options based on your current delivery area.',
                action: TextButton.icon(
                  onPressed: () => context.push('/customer/restaurants'),
                  icon: const Icon(Icons.search),
                  label: const Text('Browse'),
                ),
              ),
              const SizedBox(height: 12),
              _NearbyRestaurantsRow(state: homeState),
              const SizedBox(height: 20),
              const FoodyaSectionHeader(
                title: 'Today on Foodya',
                subtitle: 'Everything you need to order and track food faster.',
              ),
              const SizedBox(height: 12),
              GridView.count(
                crossAxisCount: 2,
                childAspectRatio: 0.95,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                children: [
                  FoodyaActionCard(
                    title: 'Find food',
                    subtitle: 'Search restaurants, dishes, and categories.',
                    icon: Icons.search,
                    onTap: () => context.push('/customer/restaurants'),
                  ),
                  FoodyaActionCard(
                    title: 'Your cart',
                    subtitle: 'Review items and continue checkout.',
                    icon: Icons.shopping_bag_outlined,
                    onTap: () => context.push('/customer/cart'),
                  ),
                  FoodyaActionCard(
                    title: 'Orders',
                    subtitle: 'Track delivery and review completed meals.',
                    icon: Icons.delivery_dining_outlined,
                    onTap: () => context.push('/customer/orders'),
                  ),
                  FoodyaActionCard(
                    title: 'AI picks',
                    subtitle: 'Get meal ideas based on your taste.',
                    icon: Icons.auto_awesome_outlined,
                    onTap: () => context.push('/customer/ai'),
                  ),
                  FoodyaActionCard(
                    title: 'Updates',
                    subtitle: 'See order and promotion notifications.',
                    icon: Icons.notifications_outlined,
                    onTap: () => context.push('/customer/notifications'),
                  ),
                  FoodyaActionCard(
                    title: 'Account',
                    subtitle: 'Manage profile and password settings.',
                    icon: Icons.person_outline,
                    onTap: () => context.push('/customer/profile'),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _chooseLocationSource(
    BuildContext context,
    CustomerHomeState state,
  ) async {
    final source = await showModalBottomSheet<String>(
      context: context,
      builder:
          (context) => SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  leading: const Icon(Icons.my_location_outlined),
                  title: const Text('Use device location'),
                  onTap: () => Navigator.of(context).pop('device'),
                ),
                ListTile(
                  leading: const Icon(Icons.edit_location_alt_outlined),
                  title: const Text('Enter latitude and longitude'),
                  onTap: () => Navigator.of(context).pop('manual'),
                ),
              ],
            ),
          ),
    );
    if (!context.mounted || source == null) {
      return;
    }

    final cubit = context.read<CustomerHomeCubit>();
    if (source == 'device') {
      await cubit.refreshLocation();
      return;
    }

    final manual = await showManualLocationSheet(
      context: context,
      initialLatitude: state.latitude,
      initialLongitude: state.longitude,
    );
    if (!context.mounted || manual == null) {
      return;
    }
    await cubit.useManualLocation(
      latitude: manual.latitude,
      longitude: manual.longitude,
    );
  }
}

class _HomeHero extends StatelessWidget {
  const _HomeHero({
    required this.locationLabel,
    required this.isRefreshingLocation,
    required this.onLocationTap,
  });

  final String locationLabel;
  final bool isRefreshingLocation;
  final VoidCallback onLocationTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Hungry now?',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w800,
              color: theme.colorScheme.onPrimaryContainer,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Find warm meals, local favorites, and fast delivery nearby.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onPrimaryContainer,
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.tonalIcon(
            onPressed: isRefreshingLocation ? null : onLocationTap,
            icon:
                isRefreshingLocation
                    ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                    : const Icon(Icons.location_on_outlined),
            label: Text(
              locationLabel,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
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
                      avatar: const Icon(Icons.star, size: 16),
                      label: Text(restaurant.rating.toStringAsFixed(1)),
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
