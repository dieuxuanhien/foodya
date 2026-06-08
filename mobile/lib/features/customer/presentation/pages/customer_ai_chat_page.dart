import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/formatters/vnd_currency_formatter.dart';
import '../../domain/models/ai_chat.dart';
import '../../domain/repositories/customer_ai_repository.dart';
import '../cubit/ai_chat_cubit.dart';
import '../cubit/ai_chat_state.dart';

class CustomerAiChatPage extends StatelessWidget {
  const CustomerAiChatPage({
    super.key,
    this.initialLatitude,
    this.initialLongitude,
  });

  final double? initialLatitude;
  final double? initialLongitude;

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              AiChatCubit(repository: context.read<CustomerAiRepository>())
                ..loadHistory(),
      child: _CustomerAiChatView(
        initialLatitude: initialLatitude,
        initialLongitude: initialLongitude,
      ),
    );
  }
}

class _CustomerAiChatView extends StatefulWidget {
  const _CustomerAiChatView({
    required this.initialLatitude,
    required this.initialLongitude,
  });

  final double? initialLatitude;
  final double? initialLongitude;

  bool get hasLocation => initialLatitude != null && initialLongitude != null;

  @override
  State<_CustomerAiChatView> createState() => _CustomerAiChatViewState();
}

class _CustomerAiChatViewState extends State<_CustomerAiChatView> {
  final _promptController = TextEditingController();

  @override
  void dispose() {
    _promptController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<AiChatCubit, AiChatState>(
      listener: (context, state) {
        final message = state.errorMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Foodya AI'),
            actions: [
              if (state.history.isNotEmpty || state.latestResponse != null)
                IconButton(
                  onPressed:
                      state.isBusy ? null : () => _confirmDelete(context),
                  tooltip: 'Delete conversation',
                  icon:
                      state.status == AiChatStatus.deleting
                          ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                          : const Icon(Icons.delete_outline),
                ),
            ],
          ),
          body: SafeArea(
            child: Column(
              children: [
                Expanded(child: _ConversationList(state: state)),
                _ChatComposer(
                  controller: _promptController,
                  isSubmitting: state.status == AiChatStatus.submitting,
                  isBusy: state.isBusy,
                  hasLocation: widget.hasLocation,
                  onSend: () => _submitPrompt(context),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _confirmDelete(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Delete conversation?'),
            content: const Text('This removes your AI chat history.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(dialogContext).pop(true),
                child: const Text('Delete'),
              ),
            ],
          ),
    );
    if (!context.mounted || confirmed != true) {
      return;
    }
    await context.read<AiChatCubit>().deleteConversation();
  }

  Future<void> _submitPrompt(BuildContext context) async {
    final prompt = _promptController.text;
    if (prompt.trim().isEmpty) {
      return;
    }
    final cubit = context.read<AiChatCubit>();
    _promptController.clear();
    await cubit.submit(
      prompt: prompt,
      lat: widget.hasLocation ? widget.initialLatitude : null,
      lng: widget.hasLocation ? widget.initialLongitude : null,
    );
  }
}

class _ConversationList extends StatelessWidget {
  const _ConversationList({required this.state});

  final AiChatState state;

  @override
  Widget build(BuildContext context) {
    if (state.status == AiChatStatus.loading && state.history.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.history.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text(
            'Ask Foodya what to eat next.',
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    final history = state.history.reversed.toList(growable: false);
    final hasPendingPrompt =
        state.status == AiChatStatus.submitting &&
        state.pendingPrompt != null &&
        state.pendingPrompt!.isNotEmpty;

    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: hasPendingPrompt ? history.length + 1 : history.length,
      separatorBuilder: (_, _) => const SizedBox(height: 16),
      itemBuilder: (context, index) {
        if (index >= history.length) {
          return _PendingExchange(prompt: state.pendingPrompt!);
        }
        final item = history[index];
        final latestResponse =
            state.latestResponse?.chatId == item.chatId
                ? state.latestResponse
                : null;
        return _ChatExchange(item: item, latestResponse: latestResponse);
      },
    );
  }
}

class _PendingExchange extends StatelessWidget {
  const _PendingExchange({required this.prompt});

  final String prompt;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _ChatBubble(text: prompt, isUser: true),
        const SizedBox(height: 8),
        const _BotTypingBubble(),
      ],
    );
  }
}

class _ChatExchange extends StatelessWidget {
  const _ChatExchange({required this.item, required this.latestResponse});

  final AiChatHistoryItem item;
  final AiChatResponse? latestResponse;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _ChatBubble(text: item.prompt, isUser: true),
        const SizedBox(height: 8),
        _ChatBubble(text: item.responseSummary, isUser: false),
        if (latestResponse != null &&
            latestResponse!.recommendations.isNotEmpty) ...[
          const SizedBox(height: 8),
          _RecommendationCards(items: latestResponse!.recommendations),
        ],
      ],
    );
  }
}

class _ChatBubble extends StatelessWidget {
  const _ChatBubble({required this.text, required this.isUser});

  final String text;
  final bool isUser;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.78,
        ),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color:
                isUser
                    ? colors.primaryContainer
                    : colors.surfaceContainerHighest,
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(18),
              topRight: const Radius.circular(18),
              bottomLeft: Radius.circular(isUser ? 18 : 4),
              bottomRight: Radius.circular(isUser ? 4 : 18),
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            child: Text(
              text,
              style: theme.textTheme.bodyMedium?.copyWith(
                color:
                    isUser
                        ? colors.onPrimaryContainer
                        : colors.onSurfaceVariant,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _BotTypingBubble extends StatelessWidget {
  const _BotTypingBubble();

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(18),
        ),
        child: const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      ),
    );
  }
}

class _RecommendationCards extends StatelessWidget {
  const _RecommendationCards({required this.items});

  final List<AiRecommendationItem> items;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: items
          .map(
            (item) => Card(
              margin: const EdgeInsets.only(left: 36, bottom: 8),
              child: ListTile(
                title: Text(item.menuItemName),
                subtitle: Text('${item.restaurantName}\n${item.reason}'),
                trailing: Text(formatVndCurrency(item.price)),
                onTap:
                    item.restaurantId.isEmpty
                        ? null
                        : () => context.push(
                          '/customer/restaurants/${item.restaurantId}',
                        ),
              ),
            ),
          )
          .toList(growable: false),
    );
  }
}

class _ChatComposer extends StatelessWidget {
  const _ChatComposer({
    required this.controller,
    required this.isSubmitting,
    required this.isBusy,
    required this.hasLocation,
    required this.onSend,
  });

  final TextEditingController controller;
  final bool isSubmitting;
  final bool isBusy;
  final bool hasLocation;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: colors.surface,
        border: Border(top: BorderSide(color: colors.outlineVariant)),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (hasLocation) ...[
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.location_on_outlined,
                    size: 16,
                    color: colors.primary,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    'Home location active',
                    style: Theme.of(
                      context,
                    ).textTheme.labelSmall?.copyWith(color: colors.primary),
                  ),
                ],
              ),
              const SizedBox(height: 8),
            ],
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: controller,
                    minLines: 1,
                    maxLines: 4,
                    enabled: !isBusy,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) {
                      if (!isBusy) {
                        onSend();
                      }
                    },
                    decoration: const InputDecoration(
                      hintText: 'Ask for dinner, budget, mood...',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: isBusy ? null : onSend,
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(48, 48),
                    padding: EdgeInsets.zero,
                  ),
                  child:
                      isSubmitting
                          ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                          : const Icon(Icons.send_outlined),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
