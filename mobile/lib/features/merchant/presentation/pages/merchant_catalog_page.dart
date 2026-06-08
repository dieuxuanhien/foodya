import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../domain/models/merchant_menu_category.dart';
import '../../domain/models/merchant_menu_category_request.dart';
import '../../domain/models/merchant_menu_item.dart';
import '../../domain/models/merchant_menu_item_request.dart';
import '../../domain/models/merchant_restaurant.dart';
import '../../domain/repositories/merchant_catalog_repository.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../cubit/merchant_catalog_cubit.dart';
import '../cubit/merchant_catalog_state.dart';

class MerchantCatalogPage extends StatelessWidget {
  const MerchantCatalogPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantCatalogCubit(
            catalogRepository: context.read<MerchantCatalogRepository>(),
            restaurantRepository: context.read<MerchantRestaurantRepository>(),
          )..load(),
      child: const _MerchantCatalogView(),
    );
  }
}

class _MerchantCatalogView extends StatefulWidget {
  const _MerchantCatalogView();

  @override
  State<_MerchantCatalogView> createState() => _MerchantCatalogViewState();
}

class _MerchantCatalogViewState extends State<_MerchantCatalogView> {
  final _categoryFormKey = GlobalKey<FormState>();
  final _itemFormKey = GlobalKey<FormState>();
  final _categoryNameController = TextEditingController();
  final _categorySortController = TextEditingController(text: '0');
  final _itemNameController = TextEditingController();
  final _itemDescriptionController = TextEditingController();
  final _itemPriceController = TextEditingController();
  final _taxonomyController = TextEditingController();
  bool _categoryActive = true;
  bool _itemActive = true;
  bool _itemAvailable = true;
  String? _itemCategoryId;

  @override
  void dispose() {
    _categoryNameController.dispose();
    _categorySortController.dispose();
    _itemNameController.dispose();
    _itemDescriptionController.dispose();
    _itemPriceController.dispose();
    _taxonomyController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<MerchantCatalogCubit, MerchantCatalogState>(
      listenWhen:
          (previous, current) =>
              previous.selectedCategory != current.selectedCategory ||
              previous.selectedItem != current.selectedItem ||
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final category = state.selectedCategory;
        if (category != null) {
          _fillCategory(category);
        }
        final item = state.selectedItem;
        if (item != null) {
          _fillItem(item);
        }

        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<MerchantCatalogCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Menu Catalog'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<MerchantCatalogCubit>().load(),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _RestaurantPicker(
                restaurants: state.restaurants,
                selectedRestaurant: state.selectedRestaurant,
                isLoading: state.isLoading,
                onSelected:
                    state.isBusy
                        ? null
                        : (restaurant) => context
                            .read<MerchantCatalogCubit>()
                            .loadRestaurantCatalog(
                              restaurant,
                              preserveSelection: false,
                            ),
              ),
              const SizedBox(height: 16),
              if (state.selectedRestaurant == null)
                const FoodyaEmptyState(
                  illustrationAsset: 'assets/illustrations/empty_storefront.png',
                  icon: Icons.storefront_outlined,
                  title: 'No restaurant selected',
                  message: 'Create a restaurant profile first.',
                )
              else ...[
                _CategorySection(
                  formKey: _categoryFormKey,
                  nameController: _categoryNameController,
                  sortController: _categorySortController,
                  active: _categoryActive,
                  selectedCategory: state.selectedCategory,
                  categories: state.categories,
                  isBusy: state.isBusy,
                  onActiveChanged:
                      (value) => setState(() => _categoryActive = value),
                  onSubmit: () => _submitCategory(context, state),
                  onClear: () {
                    _clearCategory();
                    context
                        .read<MerchantCatalogCubit>()
                        .clearCategorySelection();
                  },
                  onSelect:
                      (category) => context
                          .read<MerchantCatalogCubit>()
                          .selectCategory(category),
                  onDelete:
                      (category) => context
                          .read<MerchantCatalogCubit>()
                          .deleteCategory(category.id),
                ),
                const SizedBox(height: 16),
                _ItemSection(
                  formKey: _itemFormKey,
                  nameController: _itemNameController,
                  descriptionController: _itemDescriptionController,
                  priceController: _itemPriceController,
                  taxonomyController: _taxonomyController,
                  active: _itemActive,
                  available: _itemAvailable,
                  selectedCategoryId: _itemCategoryId,
                  selectedItem: state.selectedItem,
                  categories: state.categories,
                  items: state.items,
                  taxonomyHints:
                      state.taxonomies.map((item) => item.code).toList(),
                  isBusy: state.isBusy,
                  imageFile: state.menuItemImageFile,
                  onImageChanged:
                      (file) => context
                          .read<MerchantCatalogCubit>()
                          .setMenuItemImageFile(file),
                  onCategoryChanged:
                      (value) => setState(() => _itemCategoryId = value),
                  onActiveChanged:
                      (value) => setState(() => _itemActive = value),
                  onAvailableChanged:
                      (value) => setState(() => _itemAvailable = value),
                  onSubmit: () => _submitItem(context, state),
                  onClear: () {
                    _clearItem();
                    context.read<MerchantCatalogCubit>().clearItemSelection();
                    context
                        .read<MerchantCatalogCubit>()
                        .setMenuItemImageFile(null);
                  },
                  onSelect:
                      (item) =>
                          context.read<MerchantCatalogCubit>().selectItem(item),
                  onAvailabilityChanged:
                      (item, value) => context
                          .read<MerchantCatalogCubit>()
                          .setItemAvailability(
                            menuItemId: item.id,
                            isAvailable: value,
                          ),
                  onDelete:
                      (item) => context.read<MerchantCatalogCubit>().deleteItem(
                        item.id,
                      ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  void _fillCategory(MerchantMenuCategory category) {
    _categoryNameController.text = category.name;
    _categorySortController.text = category.sortOrder.toString();
    _categoryActive = category.active;
  }

  void _clearCategory() {
    _categoryNameController.clear();
    _categorySortController.text = '0';
    setState(() => _categoryActive = true);
  }

  void _fillItem(MerchantMenuItem item) {
    _itemNameController.text = item.name;
    _itemDescriptionController.text = item.description;
    _itemPriceController.text = item.price.toStringAsFixed(0);
    _taxonomyController.text = item.taxonomyCodes.join(', ');
    _itemCategoryId = item.categoryId;
    _itemActive = item.active;
    _itemAvailable = item.available;
  }

  void _clearItem() {
    _itemNameController.clear();
    _itemDescriptionController.clear();
    _itemPriceController.clear();
    _taxonomyController.clear();
    setState(() {
      _itemCategoryId = null;
      _itemActive = true;
      _itemAvailable = true;
    });
  }

  void _submitCategory(BuildContext context, MerchantCatalogState state) {
    if (!(_categoryFormKey.currentState?.validate() ?? false)) {
      return;
    }
    final request = MerchantMenuCategoryRequest(
      name: _categoryNameController.text.trim(),
      sortOrder: int.parse(_categorySortController.text.trim()),
      isActive: _categoryActive,
    );
    final selected = state.selectedCategory;
    if (selected == null) {
      context.read<MerchantCatalogCubit>().createCategory(request);
    } else {
      context.read<MerchantCatalogCubit>().updateCategory(
        categoryId: selected.id,
        request: request,
      );
    }
  }

  void _submitItem(BuildContext context, MerchantCatalogState state) {
    if (!(_itemFormKey.currentState?.validate() ?? false)) {
      return;
    }
    final request = MerchantMenuItemRequest(
      categoryId: _itemCategoryId ?? '',
      taxonomyCodes: _taxonomyController.text
          .split(',')
          .map((item) => item.trim().toUpperCase())
          .where((item) => item.isNotEmpty)
          .toList(growable: false),
      name: _itemNameController.text.trim(),
      description: _itemDescriptionController.text.trim(),
      price: double.parse(_itemPriceController.text.trim()),
      isActive: _itemActive,
      isAvailable: _itemAvailable,
    );
    final selected = state.selectedItem;
    if (selected == null) {
      context.read<MerchantCatalogCubit>().createItem(request);
    } else {
      context.read<MerchantCatalogCubit>().updateItem(
        menuItemId: selected.id,
        request: request,
      );
    }
  }
}

class _RestaurantPicker extends StatelessWidget {
  const _RestaurantPicker({
    required this.restaurants,
    required this.selectedRestaurant,
    required this.isLoading,
    required this.onSelected,
  });

  final List<MerchantRestaurant> restaurants;
  final MerchantRestaurant? selectedRestaurant;
  final bool isLoading;
  final ValueChanged<MerchantRestaurant>? onSelected;

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
    return DropdownButtonFormField<String>(
      value: selectedRestaurant?.id,
      decoration: const InputDecoration(labelText: 'Restaurant'),
      items: restaurants
          .map(
            (restaurant) => DropdownMenuItem(
              value: restaurant.id,
              child: Text(restaurant.name),
            ),
          )
          .toList(growable: false),
      onChanged:
          onSelected == null
              ? null
              : (id) {
                final matches = restaurants.where((item) => item.id == id);
                final restaurant = matches.isEmpty ? null : matches.first;
                if (restaurant != null) {
                  onSelected!(restaurant);
                }
              },
    );
  }
}

class _CategorySection extends StatelessWidget {
  const _CategorySection({
    required this.formKey,
    required this.nameController,
    required this.sortController,
    required this.active,
    required this.selectedCategory,
    required this.categories,
    required this.isBusy,
    required this.onActiveChanged,
    required this.onSubmit,
    required this.onClear,
    required this.onSelect,
    required this.onDelete,
  });

  final GlobalKey<FormState> formKey;
  final TextEditingController nameController;
  final TextEditingController sortController;
  final bool active;
  final MerchantMenuCategory? selectedCategory;
  final List<MerchantMenuCategory> categories;
  final bool isBusy;
  final ValueChanged<bool> onActiveChanged;
  final VoidCallback onSubmit;
  final VoidCallback onClear;
  final ValueChanged<MerchantMenuCategory> onSelect;
  final ValueChanged<MerchantMenuCategory> onDelete;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Categories', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        Form(
          key: formKey,
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    flex: 2,
                    child: TextFormField(
                      controller: nameController,
                      decoration: const InputDecoration(labelText: 'Name'),
                      validator:
                          (value) =>
                              value == null || value.trim().isEmpty
                                  ? 'Required.'
                                  : null,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: sortController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Sort'),
                      validator: (value) {
                        final parsed = int.tryParse(value?.trim() ?? '');
                        return parsed == null || parsed < 0 ? 'Invalid.' : null;
                      },
                    ),
                  ),
                ],
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                value: active,
                title: const Text('Active'),
                onChanged: isBusy ? null : onActiveChanged,
              ),
              Row(
                children: [
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: isBusy ? null : onSubmit,
                      icon: Icon(
                        selectedCategory == null
                            ? Icons.add_outlined
                            : Icons.save_outlined,
                      ),
                      label: Text(
                        selectedCategory == null ? 'Create' : 'Update',
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  IconButton.outlined(
                    onPressed: isBusy ? null : onClear,
                    icon: const Icon(Icons.clear_outlined),
                    tooltip: 'Clear',
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        if (categories.isEmpty)
          const FoodyaEmptyState(
            icon: Icons.folder_open_outlined,
            title: 'No categories yet',
            message: 'Create one before adding menu items.',
          )
        else
          SizedBox(
            height: 128,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: categories.length,
              separatorBuilder: (_, _) => const SizedBox(width: 12),
              itemBuilder:
                  (context, index) => SizedBox(
                    width: 236,
                    child: _CategoryCatalogTile(
                      category: categories[index],
                      selected: selectedCategory?.id == categories[index].id,
                      isBusy: isBusy,
                      onSelect: onSelect,
                      onDelete: onDelete,
                    ),
                  ),
            ),
          ),
      ],
    );
  }
}

class _CategoryCatalogTile extends StatelessWidget {
  const _CategoryCatalogTile({
    required this.category,
    required this.selected,
    required this.isBusy,
    required this.onSelect,
    required this.onDelete,
  });

  final MerchantMenuCategory category;
  final bool selected;
  final bool isBusy;
  final ValueChanged<MerchantMenuCategory> onSelect;
  final ValueChanged<MerchantMenuCategory> onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      color: selected ? theme.colorScheme.primaryContainer : null,
      child: InkWell(
        onTap: isBusy ? null : () => onSelect(category),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    category.active
                        ? Icons.folder_open_outlined
                        : Icons.folder_off_outlined,
                    color:
                        selected ? theme.colorScheme.onPrimaryContainer : null,
                  ),
                  const Spacer(),
                  IconButton(
                    onPressed: isBusy ? null : () => onDelete(category),
                    icon: const Icon(Icons.delete_outline),
                    tooltip: 'Delete',
                  ),
                ],
              ),
              const Spacer(),
              Text(
                category.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall,
              ),
              const SizedBox(height: 2),
              Text('Sort ${category.sortOrder}'),
            ],
          ),
        ),
      ),
    );
  }
}

class _ItemSection extends StatelessWidget {
  const _ItemSection({
    required this.formKey,
    required this.nameController,
    required this.descriptionController,
    required this.priceController,
    required this.taxonomyController,
    required this.active,
    required this.available,
    required this.selectedCategoryId,
    required this.selectedItem,
    required this.categories,
    required this.items,
    required this.taxonomyHints,
    required this.isBusy,
    required this.imageFile,
    required this.onImageChanged,
    required this.onCategoryChanged,
    required this.onActiveChanged,
    required this.onAvailableChanged,
    required this.onSubmit,
    required this.onClear,
    required this.onSelect,
    required this.onAvailabilityChanged,
    required this.onDelete,
  });

  final GlobalKey<FormState> formKey;
  final TextEditingController nameController;
  final TextEditingController descriptionController;
  final TextEditingController priceController;
  final TextEditingController taxonomyController;
  final bool active;
  final bool available;
  final String? selectedCategoryId;
  final MerchantMenuItem? selectedItem;
  final List<MerchantMenuCategory> categories;
  final List<MerchantMenuItem> items;
  final List<String> taxonomyHints;
  final bool isBusy;
  final XFile? imageFile;
  final ValueChanged<XFile?> onImageChanged;
  final ValueChanged<String?> onCategoryChanged;
  final ValueChanged<bool> onActiveChanged;
  final ValueChanged<bool> onAvailableChanged;
  final VoidCallback onSubmit;
  final VoidCallback onClear;
  final ValueChanged<MerchantMenuItem> onSelect;
  final void Function(MerchantMenuItem item, bool value) onAvailabilityChanged;
  final ValueChanged<MerchantMenuItem> onDelete;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Menu Items', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        Form(
          key: formKey,
          child: Column(
            children: [
              DropdownButtonFormField<String>(
                value:
                    categories.any(
                          (category) => category.id == selectedCategoryId,
                        )
                        ? selectedCategoryId
                        : null,
                decoration: const InputDecoration(labelText: 'Category'),
                items: categories
                    .map(
                      (category) => DropdownMenuItem(
                        value: category.id,
                        child: Text(category.name),
                      ),
                    )
                    .toList(growable: false),
                onChanged: isBusy ? null : onCategoryChanged,
                validator:
                    (value) =>
                        value == null || value.isEmpty ? 'Required.' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: nameController,
                decoration: const InputDecoration(labelText: 'Name'),
                validator:
                    (value) =>
                        value == null || value.trim().isEmpty
                            ? 'Required.'
                            : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: descriptionController,
                decoration: const InputDecoration(labelText: 'Description'),
                maxLines: 2,
                validator:
                    (value) =>
                        value == null || value.trim().isEmpty
                            ? 'Required.'
                            : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: priceController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Price'),
                validator: (value) {
                  final parsed = double.tryParse(value?.trim() ?? '');
                  return parsed == null || parsed <= 0
                      ? 'Enter a value greater than 0.'
                      : null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: taxonomyController,
                decoration: InputDecoration(
                  labelText: 'Taxonomy codes',
                  hintText: taxonomyHints.take(3).join(', '),
                ),
                validator:
                    (value) =>
                        value == null || value.trim().isEmpty
                            ? 'Required.'
                            : null,
              ),
              if (taxonomyHints.isNotEmpty) ...[
                const SizedBox(height: 8),
                _HorizontalChipList(
                  children: taxonomyHints
                      .map(
                        (code) => ActionChip(
                          label: Text(code),
                          onPressed:
                              isBusy
                                  ? null
                                  : () {
                                    final values =
                                        taxonomyController.text
                                            .split(',')
                                            .map((item) => item.trim())
                                            .where((item) => item.isNotEmpty)
                                            .toSet();
                                    values.add(code);
                                    taxonomyController.text = values.join(', ');
                                  },
                        ),
                      )
                      .toList(growable: false),
                ),
              ],
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                value: active,
                title: const Text('Active'),
                onChanged: isBusy ? null : onActiveChanged,
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                value: available,
                title: const Text('Available'),
                onChanged: isBusy ? null : onAvailableChanged,
              ),
              if (selectedItem == null) ...[
                const SizedBox(height: 12),
                ImagePickerField(
                  label: 'Item image',
                  isRequired: true,
                  pickedFile: imageFile,
                  onChanged: onImageChanged,
                ),
              ],
              Row(
                children: [
                  Expanded(
                    child: FilledButton.icon(
                      onPressed:
                          isBusy ||
                                  (selectedItem == null && imageFile == null)
                              ? null
                              : onSubmit,
                      icon: Icon(
                        selectedItem == null
                            ? Icons.add_outlined
                            : Icons.save_outlined,
                      ),
                      label: Text(selectedItem == null ? 'Create' : 'Update'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  IconButton.outlined(
                    onPressed: isBusy ? null : onClear,
                    icon: const Icon(Icons.clear_outlined),
                    tooltip: 'Clear',
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        if (items.isEmpty)
          const FoodyaEmptyState(
            icon: Icons.fastfood_outlined,
            title: 'No menu items yet',
            message: 'Create the first item for this restaurant.',
          )
        else
          SizedBox(
            height: 156,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: items.length,
              separatorBuilder: (_, _) => const SizedBox(width: 12),
              itemBuilder:
                  (context, index) => SizedBox(
                    width: 308,
                    child: _MenuCatalogTile(
                      item: items[index],
                      selected: selectedItem?.id == items[index].id,
                      isBusy: isBusy,
                      onSelect: onSelect,
                      onAvailabilityChanged: onAvailabilityChanged,
                      onDelete: onDelete,
                    ),
                  ),
            ),
          ),
      ],
    );
  }
}

class _MenuCatalogTile extends StatelessWidget {
  const _MenuCatalogTile({
    required this.item,
    required this.selected,
    required this.isBusy,
    required this.onSelect,
    required this.onAvailabilityChanged,
    required this.onDelete,
  });

  final MerchantMenuItem item;
  final bool selected;
  final bool isBusy;
  final ValueChanged<MerchantMenuItem> onSelect;
  final void Function(MerchantMenuItem item, bool value) onAvailabilityChanged;
  final ValueChanged<MerchantMenuItem> onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      color: selected ? theme.colorScheme.primaryContainer : null,
      child: InkWell(
        onTap: isBusy ? null : () => onSelect(item),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FoodyaImageSurface(
                imageUrl: item.imageUrl,
                icon: Icons.fastfood_outlined,
                height: 84,
                width: 84,
                borderRadius: 12,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          item.available
                              ? Icons.check_circle_outline
                              : Icons.pause_circle_outline,
                          size: 18,
                          color:
                              selected
                                  ? theme.colorScheme.onPrimaryContainer
                                  : null,
                        ),
                        const Spacer(),
                        Switch(
                          value: item.available,
                          onChanged:
                              isBusy
                                  ? null
                                  : (value) =>
                                      onAvailabilityChanged(item, value),
                        ),
                        IconButton(
                          onPressed: isBusy ? null : () => onDelete(item),
                          icon: const Icon(Icons.delete_outline),
                          tooltip: 'Delete',
                          visualDensity: VisualDensity.compact,
                        ),
                      ],
                    ),
                    Text(
                      item.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleSmall,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      item.categoryName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Text('${item.price.toStringAsFixed(0)} VND'),
                  ],
                ),
              ),
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
