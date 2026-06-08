import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/merchant_review.dart';
import '../../domain/repositories/merchant_restaurant_repository.dart';
import '../../domain/repositories/merchant_review_repository.dart';
import '../cubit/merchant_restaurant_selection_cubit.dart';
import '../cubit/merchant_reviews_cubit.dart';
import '../cubit/merchant_reviews_state.dart';

class MerchantReviewsPage extends StatelessWidget {
  const MerchantReviewsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => MerchantReviewsCubit(
            reviewRepository: context.read<MerchantReviewRepository>(),
            restaurantRepository: context.read<MerchantRestaurantRepository>(),
            selectionCubit: context.read<MerchantRestaurantSelectionCubit>(),
          )..load(),
      child: const _MerchantReviewsView(),
    );
  }
}

class _MerchantReviewsView extends StatefulWidget {
  const _MerchantReviewsView();

  @override
  State<_MerchantReviewsView> createState() => _MerchantReviewsViewState();
}

class _MerchantReviewsViewState extends State<_MerchantReviewsView> {
  final _responseController = TextEditingController();

  @override
  void dispose() {
    _responseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<MerchantReviewsCubit, MerchantReviewsState>(
      listenWhen:
          (previous, current) =>
              previous.selectedReview != current.selectedReview ||
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final selected = state.selectedReview;
        if (selected != null) {
          _responseController.text = selected.merchantResponse ?? '';
        }
        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<MerchantReviewsCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Review Center'),
            actions: [
              IconButton(
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<MerchantReviewsCubit>().load(),
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
              ),
            ],
          ),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (state.selectedRestaurant == null)
                const Card(
                  child: ListTile(
                    leading: Icon(Icons.storefront_outlined),
                    title: Text('No restaurant selected'),
                    subtitle: Text('Create a restaurant profile first.'),
                  ),
                )
              else ...[
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.storefront_outlined),
                    title: Text(state.selectedRestaurant!.name),
                    subtitle: Text(state.selectedRestaurant!.cuisineType),
                  ),
                ),
                const SizedBox(height: 16),
                _ReviewList(
                  reviews: state.reviews,
                  selectedReviewId: state.selectedReview?.reviewId,
                  isLoading: state.isLoading,
                  isBusy: state.isBusy,
                  onSelected:
                      (review) => context
                          .read<MerchantReviewsCubit>()
                          .selectReview(review),
                ),
                if (state.selectedReview != null) ...[
                  const SizedBox(height: 16),
                  _ResponseEditor(
                    review: state.selectedReview!,
                    controller: _responseController,
                    isSaving: state.isSaving,
                    onSubmit: () {
                      final response = _responseController.text.trim();
                      if (response.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Response is required.'),
                          ),
                        );
                        return;
                      }
                      context.read<MerchantReviewsCubit>().respond(response);
                    },
                  ),
                ],
              ],
            ],
          ),
        );
      },
    );
  }
}

class _ReviewList extends StatelessWidget {
  const _ReviewList({
    required this.reviews,
    required this.selectedReviewId,
    required this.isLoading,
    required this.isBusy,
    required this.onSelected,
  });

  final List<MerchantReview> reviews;
  final String? selectedReviewId;
  final bool isLoading;
  final bool isBusy;
  final ValueChanged<MerchantReview> onSelected;

  @override
  Widget build(BuildContext context) {
    if (isLoading && reviews.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (reviews.isEmpty) {
      return const Card(
        child: ListTile(
          leading: Icon(Icons.rate_review_outlined),
          title: Text('No reviews yet'),
          subtitle: Text('Customer feedback will appear here.'),
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Reviews', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        for (final review in reviews)
          Card(
            child: ListTile(
              selected: selectedReviewId == review.reviewId,
              onTap: isBusy ? null : () => onSelected(review),
              leading: const Icon(Icons.star_outline),
              title: Text('${review.stars}/5 - ${review.comment}'),
              subtitle: Text(
                review.merchantResponse == null ? 'No response' : 'Responded',
              ),
            ),
          ),
      ],
    );
  }
}

class _ResponseEditor extends StatelessWidget {
  const _ResponseEditor({
    required this.review,
    required this.controller,
    required this.isSaving,
    required this.onSubmit,
  });

  final MerchantReview review;
  final TextEditingController controller;
  final bool isSaving;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Order ${review.orderId}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(review.comment),
            const SizedBox(height: 12),
            TextField(
              controller: controller,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Response'),
            ),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: isSaving ? null : onSubmit,
              icon: const Icon(Icons.reply_outlined),
              label: const Text('Save Response'),
            ),
          ],
        ),
      ),
    );
  }
}
