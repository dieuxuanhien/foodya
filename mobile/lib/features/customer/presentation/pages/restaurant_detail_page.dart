import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../domain/repositories/customer_cart_repository.dart';
import '../../domain/models/restaurant_menu_item.dart';
import '../../domain/repositories/customer_catalog_repository.dart';
import '../cubit/add_to_cart_cubit.dart';
import '../cubit/add_to_cart_state.dart';
import '../cubit/restaurant_detail_cubit.dart';
import '../cubit/restaurant_detail_state.dart';

class RestaurantDetailPage extends StatelessWidget {
  const RestaurantDetailPage({super.key, required this.restaurantId});

  final String restaurantId;

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create:
              (context) => RestaurantDetailCubit(
                repository: context.read<CustomerCatalogRepository>(),
              )..load(restaurantId),
        ),
        BlocProvider(
          create:
              (context) => AddToCartCubit(
                repository: context.read<CustomerCartRepository>(),
              ),
        ),
      ],
      child: const _RestaurantDetailView(),
    );
  }
}

class _RestaurantDetailView extends StatefulWidget {
  const _RestaurantDetailView();

  @override
  State<_RestaurantDetailView> createState() => _RestaurantDetailViewState();
}

class _RestaurantDetailViewState extends State<_RestaurantDetailView> {
  final TextEditingController _menuSearchController = TextEditingController();

  @override
  void dispose() {
    _menuSearchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<RestaurantDetailCubit, RestaurantDetailState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage &&
              current.errorMessage != null,
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
        return BlocConsumer<AddToCartCubit, AddToCartState>(
          listenWhen:
              (previous, current) =>
                  previous.errorMessage != current.errorMessage ||
                  previous.infoMessage != current.infoMessage,
          listener: (context, addState) {
            final message = addState.errorMessage ?? addState.infoMessage;
            if (message == null) {
              return;
            }
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(SnackBar(content: Text(message)));
            context.read<AddToCartCubit>().clearFeedback();
          },
          builder: (context, addState) {
            if (state.isInitialLoading) {
              return Scaffold(
                appBar: AppBar(
                  title: const Text('Restaurant Detail'),
                  actions: [
                    IconButton(
                      onPressed: () => context.push('/customer/cart'),
                      icon: const Icon(Icons.shopping_cart_outlined),
                    ),
                  ],
                ),
                body: const Center(child: CircularProgressIndicator()),
              );
            }

            if (state.restaurant == null) {
              return Scaffold(
                appBar: AppBar(
                  title: const Text('Restaurant Detail'),
                  actions: [
                    IconButton(
                      onPressed: () => context.push('/customer/cart'),
                      icon: const Icon(Icons.shopping_cart_outlined),
                    ),
                  ],
                ),
                body: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(
                      state.errorMessage ??
                          'Unable to load restaurant details.',
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              );
            }

            final restaurant = state.restaurant!;

            return Scaffold(
              appBar: AppBar(
                title: const Text('Restaurant Detail'),
                actions: [
                  IconButton(
                    onPressed: () => context.push('/customer/cart'),
                    icon: const Icon(Icons.shopping_cart_outlined),
                  ),
                  IconButton(
                    onPressed: () => context.push('/customer/orders'),
                    icon: const Icon(Icons.receipt_long_outlined),
                  ),
                ],
              ),
              body: RefreshIndicator(
                onRefresh:
                    () => context.read<RestaurantDetailCubit>().refreshMenu(),
                child: ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    if (restaurant.backgroundImageUrl != null)
                      ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: Image.network(
                          restaurant.backgroundImageUrl!,
                          height: 160,
                          fit: BoxFit.cover,
                          errorBuilder:
                              (_, _, _) => Container(
                                height: 160,
                                color: Colors.black12,
                                alignment: Alignment.center,
                                child: const Icon(
                                  Icons.image_not_supported_outlined,
                                ),
                              ),
                        ),
                      ),
                    const SizedBox(height: 12),
                    if (state.errorMessage != null)
                      Card(
                        color: Theme.of(context).colorScheme.errorContainer,
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Row(
                            children: [
                              Icon(
                                Icons.error_outline,
                                color:
                                    Theme.of(
                                      context,
                                    ).colorScheme.onErrorContainer,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  state.errorMessage!,
                                  style: TextStyle(
                                    color:
                                        Theme.of(
                                          context,
                                        ).colorScheme.onErrorContainer,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed:
                                    () =>
                                        context
                                            .read<RestaurantDetailCubit>()
                                            .refreshMenu(),
                                child: const Text('Retry'),
                              ),
                            ],
                          ),
                        ),
                      ),
                    if (state.errorMessage != null) const SizedBox(height: 12),
                    Text(
                      restaurant.name,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 4),
                    Text(restaurant.cuisineType),
                    const SizedBox(height: 8),
                    _HorizontalChipList(
                      children: [
                        Chip(label: Text(restaurant.open ? 'Open' : 'Closed')),
                        Chip(
                          label: Text(
                            'Rating ${restaurant.avgRating.toStringAsFixed(1)}',
                          ),
                        ),
                        Chip(label: Text('${restaurant.reviewCount} reviews')),
                        Chip(
                          label: Text(
                            'Delivery ${restaurant.maxDeliveryKm.toStringAsFixed(1)} km',
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      restaurant.addressLine,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _menuSearchController,
                            textInputAction: TextInputAction.search,
                            onSubmitted:
                                (value) => context
                                    .read<RestaurantDetailCubit>()
                                    .updateMenuKeyword(value),
                            decoration: const InputDecoration(
                              hintText: 'Search menu',
                              prefixIcon: Icon(Icons.search),
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        DropdownButton<String>(
                          value: state.menuSort,
                          onChanged: (value) {
                            if (value != null) {
                              context
                                  .read<RestaurantDetailCubit>()
                                  .updateMenuSort(value);
                            }
                          },
                          items: const [
                            DropdownMenuItem(
                              value: 'popularity_desc',
                              child: Text('Popular'),
                            ),
                            DropdownMenuItem(
                              value: 'name_asc',
                              child: Text('Name'),
                            ),
                            DropdownMenuItem(
                              value: 'price_asc',
                              child: Text('Price ↑'),
                            ),
                            DropdownMenuItem(
                              value: 'price_desc',
                              child: Text('Price ↓'),
                            ),
                          ],
                        ),
                      ],
                    ),
                    if (state.taxonomies.isNotEmpty) ...[
                      const SizedBox(height: 10),
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
                                        .read<RestaurantDetailCubit>()
                                        .toggleTaxonomy(taxonomy.code),
                              ),
                            )
                            .toList(growable: false),
                      ),
                    ],
                    const SizedBox(height: 14),
                    if (state.status == RestaurantDetailStatus.empty)
                      const _EmptyMenuState()
                    else
                      SizedBox(
                        height: 238,
                        child: ListView.separated(
                          scrollDirection: Axis.horizontal,
                          itemCount:
                              state.menuItems.length +
                              (state.status ==
                                          RestaurantDetailStatus.loadingMore ||
                                      state.hasMore
                                  ? 1
                                  : 0),
                          separatorBuilder: (_, _) => const SizedBox(width: 12),
                          itemBuilder: (context, index) {
                            if (index < state.menuItems.length) {
                              final item = state.menuItems[index];
                              return SizedBox(
                                width: 292,
                                child: _MenuItemCard(
                                  item: item,
                                  isAdding: addState.isBusy,
                                  onAdd:
                                      addState.isBusy
                                          ? null
                                          : () => context
                                              .read<AddToCartCubit>()
                                              .addItem(
                                                menuItemId: item.id,
                                                quantity: 1,
                                              ),
                                ),
                              );
                            }

                            if (state.status ==
                                RestaurantDetailStatus.loadingMore) {
                              return const SizedBox(
                                width: 132,
                                child: Card(
                                  child: Center(
                                    child: CircularProgressIndicator(),
                                  ),
                                ),
                              );
                            }

                            return SizedBox(
                              width: 148,
                              child: OutlinedButton(
                                onPressed:
                                    () =>
                                        context
                                            .read<RestaurantDetailCubit>()
                                            .loadMoreMenu(),
                                child: const Text(
                                  'Load more menu items',
                                  textAlign: TextAlign.center,
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                    const SizedBox(height: 20),
                    Text(
                      'Reviews',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    if (state.isReviewsLoading)
                      const Center(child: CircularProgressIndicator())
                    else if (state.reviews.isEmpty)
                      const Card(
                        child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('No reviews yet.'),
                        ),
                      )
                    else
                      ...state.reviews.map(
                        (review) => Card(
                          child: ListTile(
                            leading: CircleAvatar(
                              child: Text(review.stars.toString()),
                            ),
                            title: Text(
                              review.comment.isEmpty
                                  ? 'No comment'
                                  : review.comment,
                            ),
                            subtitle:
                                review.merchantResponse == null
                                    ? null
                                    : Text(
                                      'Merchant: ${review.merchantResponse}',
                                    ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }
}

class _MenuItemCard extends StatelessWidget {
  const _MenuItemCard({
    required this.item,
    required this.isAdding,
    required this.onAdd,
  });

  final RestaurantMenuItem item;
  final bool isAdding;
  final VoidCallback? onAdd;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                item.imageUrl == null
                    ? const CircleAvatar(
                      radius: 28,
                      child: Icon(Icons.fastfood_outlined),
                    )
                    : ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: Image.network(
                        item.imageUrl!,
                        width: 56,
                        height: 56,
                        fit: BoxFit.cover,
                        errorBuilder:
                            (_, _, _) => const CircleAvatar(
                              radius: 28,
                              child: Icon(Icons.fastfood_outlined),
                            ),
                      ),
                    ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        item.description ?? 'No description',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const Spacer(),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '${item.price.toStringAsFixed(0)} VND',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleSmall,
                  ),
                ),
                Chip(
                  visualDensity: VisualDensity.compact,
                  label: Text(item.available ? 'Available' : 'Unavailable'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: FilledButton.tonalIcon(
                onPressed: item.available && !isAdding ? onAdd : null,
                icon:
                    isAdding
                        ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                        : const Icon(
                          Icons.add_shopping_cart_outlined,
                          size: 16,
                        ),
                label: const Text('Add'),
              ),
            ),
          ],
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

class _EmptyMenuState extends StatelessWidget {
  const _EmptyMenuState();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 24),
      child: Center(child: Text('No menu items match the current filters.')),
    );
  }
}
