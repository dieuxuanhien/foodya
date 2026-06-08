import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/ai_chat_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/ai_chat_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/notification_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/notification_state.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/profile_cubit.dart';
import 'package:foodya_mobile/features/customer/presentation/cubit/profile_state.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/mock_repositories.dart';
import '../../helpers/test_models.dart';

void main() {
  test('NotificationCubit loads and marks notification as read', () async {
    final repository = MockCustomerNotificationRepository();
    when(
      () => repository.listNotifications(),
    ).thenAnswer((_) async => [customerNotification()]);
    when(
      () => repository.markAsRead('notification-1'),
    ).thenAnswer((_) async => customerNotification(status: 'READ'));
    final cubit = NotificationCubit(repository: repository);

    await cubit.load();
    await cubit.markAsRead('notification-1');

    expect(cubit.state.status, NotificationStatus.success);
    expect(cubit.state.notifications.single.isRead, isTrue);
  });

  test('NotificationCubit exposes load failure', () async {
    final repository = MockCustomerNotificationRepository();
    when(() => repository.listNotifications()).thenThrow(Exception('network'));
    final cubit = NotificationCubit(repository: repository);

    await cubit.load();

    expect(cubit.state.status, NotificationStatus.failure);
    expect(cubit.state.errorMessage, isNotEmpty);
  });

  test(
    'ProfileCubit loads, saves, changes password, and reports failures',
    () async {
      final repository = MockCustomerProfileRepository();
      when(() => repository.me()).thenAnswer((_) async => userProfile());
      when(
        () => repository.updateProfile(
          fullName: any(named: 'fullName'),
          email: any(named: 'email'),
          phoneNumber: any(named: 'phoneNumber'),
          avatarUrl: any(named: 'avatarUrl'),
        ),
      ).thenAnswer((_) async => userProfile());
      when(
        () => repository.changePassword(
          currentPassword: any(named: 'currentPassword'),
          newPassword: any(named: 'newPassword'),
          confirmPassword: any(named: 'confirmPassword'),
        ),
      ).thenAnswer((_) async {});
      final cubit = ProfileCubit(repository: repository);

      await cubit.load();
      await cubit.save(
        fullName: ' Alice Nguyen ',
        email: 'alice@example.com',
        phoneNumber: '0900000000',
      );
      await cubit.changePassword(
        currentPassword: 'old',
        newPassword: 'new',
        confirmPassword: 'new',
      );

      expect(cubit.state.status, ProfileStatus.success);
      expect(cubit.state.infoMessage, 'Password updated.');

      when(() => repository.me()).thenThrow(Exception('network'));
      await cubit.load();

      expect(cubit.state.status, ProfileStatus.failure);
      expect(cubit.state.errorMessage, isNotEmpty);
    },
  );

  test(
    'AiChatCubit loads history, submits prompt, and ignores blank prompts',
    () async {
      final repository = MockCustomerAiRepository();
      when(
        () => repository.history(),
      ).thenAnswer((_) async => [aiHistoryItem()]);
      when(
        () => repository.createChat(
          prompt: any(named: 'prompt'),
          lat: any(named: 'lat'),
          lng: any(named: 'lng'),
        ),
      ).thenAnswer((_) async => aiChatResponse());
      final cubit = AiChatCubit(repository: repository);

      await cubit.loadHistory();
      await cubit.submit(prompt: '  Dinner  ', lat: 10.77, lng: 106.7);
      await cubit.submit(prompt: '   ');

      expect(cubit.state.status, AiChatStatus.success);
      expect(cubit.state.latestResponse?.responseSummary, 'Try pho.');
      expect(cubit.state.history.first.prompt, 'Dinner');
      verify(
        () => repository.createChat(prompt: 'Dinner', lat: 10.77, lng: 106.7),
      ).called(1);
    },
  );

  test('AiChatCubit reports submit failure', () async {
    final repository = MockCustomerAiRepository();
    when(
      () => repository.createChat(
        prompt: any(named: 'prompt'),
        lat: any(named: 'lat'),
        lng: any(named: 'lng'),
      ),
    ).thenThrow(Exception('ai'));
    final cubit = AiChatCubit(repository: repository);

    await cubit.submit(prompt: 'Dinner');

    expect(cubit.state.status, AiChatStatus.failure);
    expect(cubit.state.errorMessage, isNotEmpty);
  });
}
