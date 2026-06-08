import '../../../../core/auth/auth_session_recovery.dart';
import '../../domain/models/ai_chat.dart';
import '../../domain/repositories/customer_ai_repository.dart';
import '../data_sources/customer_ai_remote_data_source.dart';

class HttpCustomerAiRepository implements CustomerAiRepository {
  HttpCustomerAiRepository({
    required CustomerAiRemoteDataSource remoteDataSource,
    required AuthSessionRecovery sessionRecovery,
  }) : _remoteDataSource = remoteDataSource,
       _sessionRecovery = sessionRecovery;

  final CustomerAiRemoteDataSource _remoteDataSource;
  final AuthSessionRecovery _sessionRecovery;

  @override
  Future<List<AiChatHistoryItem>> history() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.history(accessToken: accessToken);
    });
  }

  @override
  Future<AiChatResponse> createChat({
    required String prompt,
    double? lat,
    double? lng,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.createChat(
        accessToken: accessToken,
        prompt: prompt,
        lat: lat,
        lng: lng,
      );
    });
  }

  @override
  Future<void> deleteConversation() {
    return _sessionRecovery.runAuthorized((accessToken) {
      return _remoteDataSource.deleteConversation(accessToken: accessToken);
    });
  }
}
