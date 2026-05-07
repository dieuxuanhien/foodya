import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/models/restaurant_search_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../cubit/restaurant_browse_cubit.dart';
import '../cubit/restaurant_browse_state.dart';

class RestaurantBrowsePage extends StatelessWidget {
  const RestaurantBrowsePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => RestaurantBrowseCubit(
            repository: context.read<CustomerCatalogRepository>(),
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
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
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
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
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
}

class _SearchSection extends StatelessWidget {
  const _SearchSection({
    required this.controller,
    required this.state,
    required this.onSearch,
    required this.onSortChanged,
  });

  final TextEditingController controller;
  final RestaurantBrowseState state;
  final ValueChanged<String> onSearch;
  final ValueChanged<String> onSortChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: TextField(
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
        ),
        const SizedBox(width: 8),
        DropdownButton<String>(
          value: state.sort,
          onChanged: (value) {
            if (value != null) {
              onSortChanged(value);
            }
          },
          items: const [
            DropdownMenuItem(value: 'relevance', child: Text('Relevance')),
            DropdownMenuItem(value: 'rating_desc', child: Text('Top rated')),
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

    return Column(
      children: [
        ...state.restaurants.map(
          (restaurant) => Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: _RestaurantCard(restaurant: restaurant),
          ),
        ),
        if (state.status == RestaurantBrowseStatus.loadingMore)
          const Padding(
            padding: EdgeInsets.all(12),
            child: CircularProgressIndicator(),
          )
        else if (state.hasMore)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: OutlinedButton(
              onPressed: () => context.read<RestaurantBrowseCubit>().loadMore(),
              child: const Text('Load more'),
            ),
          ),
      ],
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
