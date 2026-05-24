import 'package:equatable/equatable.dart';

import '../../domain/models/ai_chat.dart';

enum AiChatStatus { initial, loading, success, submitting, failure }

class AiChatState extends Equatable {
  const AiChatState({
    required this.status,
    required this.history,
    this.latestResponse,
    this.errorMessage,
  });

  const AiChatState.initial()
    : this(status: AiChatStatus.initial, history: const []);

  final AiChatStatus status;
  final List<AiChatHistoryItem> history;
  final AiChatResponse? latestResponse;
  final String? errorMessage;

  bool get isBusy =>
      status == AiChatStatus.loading || status == AiChatStatus.submitting;

  AiChatState copyWith({
    AiChatStatus? status,
    List<AiChatHistoryItem>? history,
    AiChatResponse? latestResponse,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AiChatState(
      status: status ?? this.status,
      history: history ?? this.history,
      latestResponse: latestResponse ?? this.latestResponse,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props => [status, history, latestResponse, errorMessage];
}
