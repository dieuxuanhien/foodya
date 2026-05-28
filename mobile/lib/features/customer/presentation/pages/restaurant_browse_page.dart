import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../../../../core/location/geolocation_service.dart';
import '../cubit/restaurant_browse_cubit.dart';
import '../cubit/restaurant_browse_state.dart';
import '../widgets/manual_location_sheet.dart';

class RestaurantBrowsePage extends StatelessWidget {
  const RestaurantBrowsePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => RestaurantBrowseCubit(
            repository: context.read<CustomerCatalogRepository>(),
            geolocationService: context.read<GeolocationService>(),
          )..initialize(),
      child: const _RestaurantBrowseView(),
    );
  }
}

class _RestaurantBrowseView extends StatefulWidget {
  const _RestaurantBrowseView();

  @override
  State<_RestaurantBrowseView> createState() => _RestaurantBrowseViewState();
}

class _RestaurantBrowseViewState extends State<_RestaurantBrowseView> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover Restaurants'),
        actions: [
          IconButton(
            onPressed:
                () => context.read<RestaurantBrowseCubit>().clearFilters(),
            icon: const Icon(Icons.filter_alt_off_outlined),
            tooltip: 'Clear filters',
          ),
        ],
      ),
      body: BlocConsumer<RestaurantBrowseCubit, RestaurantBrowseState>(
        listenWhen:
            (previous, current) =>
                previous.errorMessage != current.errorMessage &&
                current.errorMessage != null &&
                current.restaurants.isNotEmpty,
        listener: (context, state) {
          final message = state.errorMessage;
          if (message == null) {
            return;
          }
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
        },
        builder: (context, state) {
          return RefreshIndicator(
            onRefresh: () => context.read<RestaurantBrowseCubit>().refresh(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _SearchSection(
                  controller: _searchController,
                  state: state,
                  onSearch:
                      (keyword) => context
                          .read<RestaurantBrowseCubit>()
                          .applyKeyword(keyword),
                  onSortChanged:
                      (sort) => context
                          .read<RestaurantBrowseCubit>()
                          .changeSort(sort),
                  onNearbyToggle:
                      (enabled) => _toggleNearby(context, state, enabled),
                ),
                const SizedBox(height: 12),
                _HorizontalChipList(
                  children: [
                    FilterChip(
                      label: const Text('Open now'),
                      selected: state.openNow == true,
                      onSelected:
                          (enabled) => context
                              .read<RestaurantBrowseCubit>()
                              .toggleOpenNow(enabled),
                    ),
                    ChoiceChip(
                      label: const Text('Rating 4.0+'),
                      selected: state.minRating == 4.0,
                      onSelected:
                          (selected) => context
                              .read<RestaurantBrowseCubit>()
                              .setMinRating(selected ? 4.0 : null),
                    ),
                    ChoiceChip(
                      label: const Text('Rating 4.5+'),
                      selected: state.minRating == 4.5,
                      onSelected:
                          (selected) => context
                              .read<RestaurantBrowseCubit>()
                              .setMinRating(selected ? 4.5 : null),
                    ),
                  ],
                ),
                if (state.taxonomies.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  const Text(
                    'Categories',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 8),
                  _HorizontalChipList(
                    children: state.taxonomies
                        .map(
                          (taxonomy) => FilterChip(
                            label: Text(taxonomy.displayName),
                            selected: state.selectedTaxonomyCodes.contains(
                              taxonomy.code,
                            ),
                            onSelected:
                                (_) => context
                                    .read<RestaurantBrowseCubit>()
                                    .toggleTaxonomy(taxonomy.code),
                          ),
                        )
                        .toList(growable: false),
                  ),
                ],
                const SizedBox(height: 16),
                _ResultSection(state: state),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _toggleNearby(
    BuildContext context,
    RestaurantBrowseState state,
    bool enabled,
  ) async {
    final cubit = context.read<RestaurantBrowseCubit>();
    if (!enabled) {
      await cubit.toggleNearby(false);
      return;
    }

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

    if (source == 'device') {
      await cubit.toggleNearby(true);
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
    await cubit.useManualNearbyLocation(
      latitude: manual.latitude,
      longitude: manual.longitude,
    );
  }
}

class _SearchSection extends StatelessWidget {
  const _SearchSection({
    required this.controller,
    required this.state,
    required this.onSearch,
    required this.onSortChanged,
    required this.onNearbyToggle,
  });

  final TextEditingController controller;
  final RestaurantBrowseState state;
  final ValueChanged<String> onSearch;
  final ValueChanged<String> onSortChanged;
  final ValueChanged<bool> onNearbyToggle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: controller,
          textInputAction: TextInputAction.search,
          onSubmitted: onSearch,
          decoration: InputDecoration(
            hintText: 'Search by restaurant or menu item',
            prefixIcon: const Icon(Icons.search),
            suffixIcon:
                state.keyword.isEmpty
                    ? null
                    : IconButton(
                      onPressed: () {
                        controller.clear();
                        onSearch('');
                      },
                      icon: const Icon(Icons.clear),
                    ),
          ),
        ),
        const SizedBox(height: 12),
        _HorizontalChipList(
          children: [
            FilterChip(
              avatar: const Icon(Icons.near_me_outlined),
              label: const Text('Nearby'),
              selected: state.isNearby,
              onSelected: onNearbyToggle,
            ),
            SizedBox(
              width: 220,
              child: DropdownButtonFormField<String>(
                value: state.sort,
                decoration: const InputDecoration(
                  labelText: 'Sort by',
                  prefixIcon: Icon(Icons.sort),
                ),
                onChanged: (value) {
                  if (value != null) {
                    onSortChanged(value);
                  }
                },
                items: [
                  const DropdownMenuItem(
                    value: 'relevance',
                    child: Text('Relevance'),
                  ),
                  const DropdownMenuItem(
                    value: 'rating_desc',
                    child: Text('Top rated'),
                  ),
                  if (state.latitude != null && state.longitude != null)
                    const DropdownMenuItem(
                      value: 'distance_asc',
                      child: Text('Closest'),
                    ),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _ResultSection extends StatelessWidget {
  const _ResultSection({required this.state});

  final RestaurantBrowseState state;

  @override
  Widget build(BuildContext context) {
    if (state.isInitialLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (state.status == RestaurantBrowseStatus.failure) {
      return _EmptyState(
        title: 'Unable to load restaurants',
        subtitle:
            state.errorMessage ?? 'Please check your network and try again.',
      );
    }

    if (state.status == RestaurantBrowseStatus.empty) {
      return const _EmptyState(
        title: 'No restaurants found',
        subtitle: 'Try adjusting keyword or filters.',
      );
    }

    return SizedBox(
      height: 224,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount:
            state.restaurants.length +
            (state.status == RestaurantBrowseStatus.loadingMore || state.hasMore
                ? 1
                : 0),
        separatorBuilder: (_, _) => const SizedBox(width: 12),
        itemBuilder: (context, index) {
          if (index < state.restaurants.length) {
            return SizedBox(
              width: 308,
              child: _RestaurantCard(restaurant: state.restaurants[index]),
            );
          }

          if (state.status == RestaurantBrowseStatus.loadingMore) {
            return const SizedBox(
              width: 132,
              child: Card(child: Center(child: CircularProgressIndicator())),
            );
          }

          return SizedBox(
            width: 132,
            child: OutlinedButton(
              onPressed: () => context.read<RestaurantBrowseCubit>().loadMore(),
              child: const Text('Load more', textAlign: TextAlign.center),
            ),
          );
        },
      ),
    );
  }
}

class _RestaurantCard extends StatelessWidget {
  const _RestaurantCard({required this.restaurant});

  final RestaurantSearchItem restaurant;

  @override
  Widget build(BuildContext context) {
    final matchedNames = restaurant.matchedItems.take(3).map((e) => e.name);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap:
            () => context.push(
              '/customer/restaurants/${restaurant.restaurantId}',
            ),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    backgroundImage:
                        restaurant.avatarImageUrl == null
                            ? null
                            : NetworkImage(restaurant.avatarImageUrl!),
                    child:
                        restaurant.avatarImageUrl == null
                            ? const Icon(Icons.storefront_outlined)
                            : null,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          restaurant.restaurantName,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        Text(
                          restaurant.cuisine,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  Chip(
                    label: Text(restaurant.openStatus ? 'Open' : 'Closed'),
                    visualDensity: VisualDensity.compact,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [
                  _InfoTag(
                    icon: Icons.star,
                    text: restaurant.rating.toStringAsFixed(1),
                  ),
                  _InfoTag(
                    icon: Icons.delivery_dining,
                    text:
                        'Up to ${restaurant.maxDeliveryKm.toStringAsFixed(1)} km',
                  ),
                  if (restaurant.distanceKm != null)
                    _InfoTag(
                      icon: Icons.near_me_outlined,
                      text: '${restaurant.distanceKm!.toStringAsFixed(1)} km',
                    ),
                ],
              ),
              if (matchedNames.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  'Matched items: ${matchedNames.join(', ')}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _HorizontalChipList extends StatelessWidget {
  const _HorizontalChipList({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (var index = 0; index < children.length; index++) ...[
            if (index > 0) const SizedBox(width: 8),
            children[index],
          ],
        ],
      ),
    );
  }
}

class _InfoTag extends StatelessWidget {
  const _InfoTag({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Chip(
      visualDensity: VisualDensity.compact,
      avatar: Icon(icon, size: 16),
      label: Text(text),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.search_off_outlined, size: 36),
            const SizedBox(height: 12),
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 6),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}
