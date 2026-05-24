import '../models/ai_chat.dart';

abstract class CustomerAiRepository {
  Future<List<AiChatHistoryItem>> history();

  Future<AiChatResponse> createChat({
    required String prompt,
    double? lat,
    double? lng,
  });
}
