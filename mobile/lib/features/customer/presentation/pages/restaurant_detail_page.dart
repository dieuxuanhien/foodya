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
                    Wrap(
                      spacing: 8,
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
                    else ...[
                      ...state.menuItems.map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: _MenuItemCard(
                            item: item,
                            isAdding: addState.isBusy,
                            onAdd:
                                addState.isBusy
                                    ? null
                                    : () =>
                                        context.read<AddToCartCubit>().addItem(
                                          menuItemId: item.id,
                                          quantity: 1,
                                        ),
                          ),
                        ),
                      ),
                      if (state.status == RestaurantDetailStatus.loadingMore)
                        const Padding(
                          padding: EdgeInsets.all(12),
                          child: CircularProgressIndicator(),
                        )
                      else if (state.hasMore)
                        OutlinedButton(
                          onPressed:
                              () =>
                                  context
                                      .read<RestaurantDetailCubit>()
                                      .loadMoreMenu(),
                          child: const Text('Load more menu items'),
                        ),
                    ],
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
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.all(12),
        leading:
            item.imageUrl == null
                ? const CircleAvatar(child: Icon(Icons.fastfood_outlined))
                : ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    item.imageUrl!,
                    width: 48,
                    height: 48,
                    fit: BoxFit.cover,
                    errorBuilder:
                        (_, _, _) => const CircleAvatar(
                          child: Icon(Icons.fastfood_outlined),
                        ),
                  ),
                ),
        title: Text(item.name),
        subtitle: Text(item.description ?? 'No description'),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              '${item.price.toStringAsFixed(0)} VND',
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 4),
            Text(
              item.available ? 'Available' : 'Unavailable',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 8),
            FilledButton.tonalIcon(
              onPressed: item.available && !isAdding ? onAdd : null,
              icon:
                  isAdding
                      ? const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                      : const Icon(Icons.add_shopping_cart_outlined, size: 16),
              label: const Text('Add'),
            ),
          ],
        ),
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
