import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/models/ai_chat.dart';
import '../../domain/repositories/customer_ai_repository.dart';
import '../cubit/ai_chat_cubit.dart';
import '../cubit/ai_chat_state.dart';

class CustomerAiChatPage extends StatelessWidget {
  const CustomerAiChatPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) =>
              AiChatCubit(repository: context.read<CustomerAiRepository>())
                ..loadHistory(),
      child: const _CustomerAiChatView(),
    );
  }
}

class _CustomerAiChatView extends StatefulWidget {
  const _CustomerAiChatView();

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
          appBar: AppBar(title: const Text('AI Recommendations')),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              TextField(
                controller: _promptController,
                minLines: 2,
                maxLines: 4,
                decoration: const InputDecoration(
                  labelText: 'What are you craving?',
                ),
              ),
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed:
                    state.isBusy
                        ? null
                        : () {
                          context.read<AiChatCubit>().submit(
                            prompt: _promptController.text,
                          );
                          _promptController.clear();
                        },
                icon:
                    state.status == AiChatStatus.submitting
                        ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                        : const Icon(Icons.auto_awesome_outlined),
                label: const Text('Ask AI'),
              ),
              const SizedBox(height: 16),
              if (state.latestResponse != null)
                _AiResponseCard(response: state.latestResponse!),
              const SizedBox(height: 16),
              Text('History', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              if (state.status == AiChatStatus.loading)
                const Center(child: CircularProgressIndicator())
              else if (state.history.isEmpty)
                const Card(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Text('No AI chats yet.'),
                  ),
                )
              else
                ...state.history.map(
                  (item) => Card(
                    child: ListTile(
                      title: Text(item.prompt),
                      subtitle: Text(item.responseSummary),
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}

class _AiResponseCard extends StatelessWidget {
  const _AiResponseCard({required this.response});

  final AiChatResponse response;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(response.responseSummary),
            const SizedBox(height: 12),
            ...response.recommendations.map(
              (item) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(item.menuItemName),
                subtitle: Text('${item.restaurantName}\n${item.reason}'),
                trailing: Text('${item.price.toStringAsFixed(0)} VND'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
