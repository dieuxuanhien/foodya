import 'package:equatable/equatable.dart';

import '../../domain/models/ai_chat.dart';

enum AiChatStatus { initial, loading, success, submitting, deleting, failure }

class AiChatState extends Equatable {
  const AiChatState({
    required this.status,
    required this.history,
    this.latestResponse,
    this.errorMessage,
    this.pendingPrompt,
  });

  const AiChatState.initial()
    : this(status: AiChatStatus.initial, history: const []);

  final AiChatStatus status;
  final List<AiChatHistoryItem> history;
  final AiChatResponse? latestResponse;
  final String? errorMessage;
  final String? pendingPrompt;

  bool get isBusy =>
      status == AiChatStatus.loading ||
      status == AiChatStatus.submitting ||
      status == AiChatStatus.deleting;

  AiChatState copyWith({
    AiChatStatus? status,
    List<AiChatHistoryItem>? history,
    AiChatResponse? latestResponse,
    String? errorMessage,
    String? pendingPrompt,
    bool clearError = false,
    bool clearLatestResponse = false,
    bool clearPendingPrompt = false,
  }) {
    return AiChatState(
      status: status ?? this.status,
      history: history ?? this.history,
      latestResponse:
          clearLatestResponse ? null : (latestResponse ?? this.latestResponse),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      pendingPrompt:
          clearPendingPrompt ? null : (pendingPrompt ?? this.pendingPrompt),
    );
  }

  @override
  List<Object?> get props => [
    status,
    history,
    latestResponse,
    errorMessage,
    pendingPrompt,
  ];
}
