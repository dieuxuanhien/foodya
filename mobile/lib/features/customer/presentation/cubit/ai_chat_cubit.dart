import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/models/ai_chat.dart';
import '../../domain/repositories/customer_ai_repository.dart';
import 'ai_chat_state.dart';

class AiChatCubit extends Cubit<AiChatState> {
  AiChatCubit({required CustomerAiRepository repository})
    : _repository = repository,
      super(const AiChatState.initial());

  final CustomerAiRepository _repository;

  Future<void> loadHistory() async {
    emit(state.copyWith(status: AiChatStatus.loading, clearError: true));
    try {
      final history = await _repository.history();
      emit(
        state.copyWith(
          status: AiChatStatus.success,
          history: history,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to load AI chat history.',
      );
      emit(
        state.copyWith(
          status: AiChatStatus.failure,
          errorMessage: presentation.message,
        ),
      );
    }
  }

  Future<void> submit({
    required String prompt,
    double? lat,
    double? lng,
  }) async {
    if (state.isBusy || prompt.trim().isEmpty) {
      return;
    }
    final normalizedPrompt = prompt.trim();
    emit(
      state.copyWith(
        status: AiChatStatus.submitting,
        pendingPrompt: normalizedPrompt,
        clearError: true,
      ),
    );
    try {
      final response = await _repository.createChat(
        prompt: normalizedPrompt,
        lat: lat,
        lng: lng,
      );
      final nextHistory = [
        AiChatHistoryItem(
          chatId: response.chatId,
          prompt: response.prompt,
          responseSummary: response.responseSummary,
          createdAt: response.createdAt,
        ),
        ...state.history,
      ];
      emit(
        state.copyWith(
          status: AiChatStatus.success,
          latestResponse: response,
          history: nextHistory,
          clearError: true,
          clearPendingPrompt: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to get recommendations.',
      );
      emit(
        state.copyWith(
          status: AiChatStatus.failure,
          errorMessage: presentation.message,
          clearPendingPrompt: true,
        ),
      );
    }
  }

  Future<void> deleteConversation() async {
    if (state.isBusy) {
      return;
    }
    emit(state.copyWith(status: AiChatStatus.deleting, clearError: true));
    try {
      await _repository.deleteConversation();
      emit(
        state.copyWith(
          status: AiChatStatus.success,
          history: const [],
          clearLatestResponse: true,
          clearPendingPrompt: true,
          clearError: true,
        ),
      );
    } catch (error) {
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Unable to delete AI conversation.',
      );
      emit(
        state.copyWith(
          status: AiChatStatus.failure,
          errorMessage: presentation.message,
          clearPendingPrompt: true,
        ),
      );
    }
  }
}
