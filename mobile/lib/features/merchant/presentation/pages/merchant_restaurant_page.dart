import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/models/merchant_restaurant_request.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../cubit/merchant_restaurant_cubit.dart';
import '../cubit/merchant_restaurant_state.dart';

class MerchantRestaurantPage extends StatelessWidget {
  const MerchantRestaurantPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantRestaurantCubit(
            repository: context.read<MerchantRestaurantRepository>(),
          )..loadRestaurants(),
      child: const _MerchantRestaurantView(),
    );
  }
}

class _MerchantRestaurantView extends StatefulWidget {
  const _MerchantRestaurantView();

  @override
  State<_MerchantRestaurantView> createState() =>
      _MerchantRestaurantViewState();
}

class _MerchantRestaurantViewState extends State<_MerchantRestaurantView> {
  final _formKey = GlobalKey<FormState>();
  final _restaurantIdController = TextEditingController();
  final _nameController = TextEditingController();
  final _cuisineController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _addressController = TextEditingController();
  final _latitudeController = TextEditingController();
  final _longitudeController = TextEditingController();
  final _maxDeliveryController = TextEditingController(text: '5');
  bool _isOpen = true;

  @override
  void dispose() {
    _restaurantIdController.dispose();
    _nameController.dispose();
    _cuisineController.dispose();
    _descriptionController.dispose();
    _addressController.dispose();
    _latitudeController.dispose();
    _longitudeController.dispose();
    _maxDeliveryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<MerchantRestaurantCubit, MerchantRestaurantState>(
      listenWhen:
          (previous, current) =>
              previous.restaurant != current.restaurant ||
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final restaurant = state.restaurant;
        if (restaurant != null) {
          _fillFromRestaurant(restaurant);
        }

        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<MerchantRestaurantCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Restaurant Console'),
            actions: [
              IconButton(
                onPressed: () => context.push('/merchant/catalog'),
                icon: const Icon(Icons.fastfood_outlined),
                tooltip: 'Catalog',
              ),
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () =>
                            context
                                .read<MerchantRestaurantCubit>()
                                .loadRestaurants(),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _RestaurantList(
                restaurants: state.restaurants,
                selectedRestaurantId: state.restaurant?.id,
                isLoading: state.isLoading,
                onSelected: (restaurant) {
                  context.read<MerchantRestaurantCubit>().selectRestaurant(
                    restaurant,
                  );
                },
              ),
              const SizedBox(height: 16),
              Form(
                key: _formKey,
                child: Column(
                  children: [
                    TextFormField(
                      controller: _restaurantIdController,
                      readOnly: true,
                      decoration: const InputDecoration(
                        labelText: 'Restaurant ID for update',
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _nameController,
                      decoration: const InputDecoration(labelText: 'Name'),
                      validator: _required,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _cuisineController,
                      decoration: const InputDecoration(
                        labelText: 'Cuisine types',
                        hintText: 'Vietnamese, Noodles',
                      ),
                      validator: _required,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _descriptionController,
                      decoration: const InputDecoration(
                        labelText: 'Description',
                      ),
                      maxLines: 3,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _addressController,
                      decoration: const InputDecoration(labelText: 'Address'),
                      validator: _required,
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: TextFormField(
                            controller: _latitudeController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: 'Latitude',
                            ),
                            validator: _number,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: TextFormField(
                            controller: _longitudeController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: 'Longitude',
                            ),
                            validator: _number,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _maxDeliveryController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        labelText: 'Max delivery km',
                      ),
                      validator: _positiveNumber,
                    ),
                    const SizedBox(height: 12),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      value: _isOpen,
                      title: const Text('Open for orders'),
                      onChanged:
                          state.isBusy
                              ? null
                              : (value) => setState(() => _isOpen = value),
                    ),
                    const SizedBox(height: 16),
                    ImagePickerField(
                      label: 'Background image',
                      pickedFile: state.backgroundImageFile,
                      currentUrl: state.restaurant?.backgroundImageUrl,
                      onChanged:
                          (file) => context
                              .read<MerchantRestaurantCubit>()
                              .setBackgroundImageFile(file),
                    ),
                    const SizedBox(height: 12),
                    ImagePickerField(
                      label: 'Avatar / logo',
                      pickedFile: state.avatarImageFile,
                      currentUrl: state.restaurant?.avatarImageUrl,
                      onChanged:
                          (file) => context
                              .read<MerchantRestaurantCubit>()
                              .setAvatarImageFile(file),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.icon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _createRestaurant(context),
                            icon: const Icon(Icons.add_business_outlined),
                            label: const Text('Create'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: FilledButton.tonalIcon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _updateRestaurant(context),
                            icon: const Icon(Icons.save_outlined),
                            label: const Text('Update'),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed:
                          state.isBusy
                              ? null
                              : () => _toggleOpen(context, !_isOpen),
                      icon: Icon(
                        _isOpen
                            ? Icons.storefront_outlined
                            : Icons.store_outlined,
                      ),
                      label: Text(
                        _isOpen ? 'Close restaurant' : 'Open restaurant',
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _fillFromRestaurant(MerchantRestaurant restaurant) {
    _restaurantIdController.text = restaurant.id;
    _nameController.text = restaurant.name;
    _cuisineController.text = restaurant.cuisineType;
    _descriptionController.text = restaurant.description;
    _addressController.text = restaurant.addressLine;
    _latitudeController.text = restaurant.latitude.toString();
    _longitudeController.text = restaurant.longitude.toString();
    _maxDeliveryController.text = restaurant.maxDeliveryKm.toString();
    _isOpen = restaurant.open;
  }

  String? _required(String? value) {
    return value == null || value.trim().isEmpty ? 'Required.' : null;
  }

  String? _number(String? value) {
    if (value == null || double.tryParse(value.trim()) == null) {
      return 'Enter a number.';
    }
    return null;
  }

  String? _positiveNumber(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || parsed <= 0) {
      return 'Enter a value greater than 0.';
    }
    return null;
  }

  MerchantRestaurantRequest? _request() {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return null;
    }
    final cuisines = _cuisineController.text
        .split(',')
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList(growable: false);
    return MerchantRestaurantRequest(
      name: _nameController.text,
      cuisineType: cuisines.isEmpty ? _cuisineController.text : cuisines.first,
      cuisineTypes: cuisines,
      description: _descriptionController.text,
      addressLine: _addressController.text,
      latitude: double.parse(_latitudeController.text.trim()),
      longitude: double.parse(_longitudeController.text.trim()),
      maxDeliveryKm: double.parse(_maxDeliveryController.text.trim()),
      isOpen: _isOpen,
    );
  }

  void _createRestaurant(BuildContext context) {
    final request = _request();
    if (request == null) {
      return;
    }
    context.read<MerchantRestaurantCubit>().create(request);
  }

  void _updateRestaurant(BuildContext context) {
    final restaurantId = _restaurantIdController.text.trim();
    final request = _request();
    if (request == null || restaurantId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Restaurant ID is required for update.')),
      );
      return;
    }
    context.read<MerchantRestaurantCubit>().update(
      restaurantId: restaurantId,
      request: request,
    );
  }

  void _toggleOpen(BuildContext context, bool nextOpen) {
    final restaurantId = _restaurantIdController.text.trim();
    final request = _request();
    if (request == null || restaurantId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Restaurant ID is required.')),
      );
      return;
    }
    context.read<MerchantRestaurantCubit>().setOpen(
      restaurantId: restaurantId,
      request: request,
      isOpen: nextOpen,
    );
  }
}

class _RestaurantSummary extends StatelessWidget {
  const _RestaurantSummary({
    required this.restaurant,
    required this.selected,
    required this.onTap,
  });

  final MerchantRestaurant restaurant;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        selected: selected,
        onTap: onTap,
        leading: Icon(
          restaurant.open
              ? Icons.storefront_outlined
              : Icons.store_mall_directory_outlined,
        ),
        title: Text(restaurant.name),
        subtitle: Text('${restaurant.status} - ${restaurant.cuisineType}'),
        trailing: Text(restaurant.open ? 'Open' : 'Closed'),
      ),
    );
  }
}

class _RestaurantList extends StatelessWidget {
  const _RestaurantList({
    required this.restaurants,
    required this.selectedRestaurantId,
    required this.isLoading,
    required this.onSelected,
  });

  final List<MerchantRestaurant> restaurants;
  final String? selectedRestaurantId;
  final bool isLoading;
  final ValueChanged<MerchantRestaurant> onSelected;

  @override
  Widget build(BuildContext context) {
    if (isLoading && restaurants.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (restaurants.isEmpty) {
      return const Card(
        child: ListTile(
          leading: Icon(Icons.storefront_outlined),
          title: Text('No restaurants yet'),
          subtitle: Text('Create your first restaurant below.'),
        ),
      );
    }
    return Column(
      children: [
        for (final restaurant in restaurants)
          _RestaurantSummary(
            restaurant: restaurant,
            selected: restaurant.id == selectedRestaurantId,
            onTap: () => onSelected(restaurant),
          ),
      ],
    );
  }
}
