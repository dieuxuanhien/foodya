import 'dart:developer' as developer;
import 'dart:async';
import 'dart:convert';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;

import '../../firebase_options.dart';
import '../auth/auth_session_recovery.dart';
import '../network/api_exception.dart';

@pragma('vm:entry-point')
Future<void> foodyaFirebaseMessagingBackgroundHandler(
  RemoteMessage message,
) async {
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  developer.log(
    'Background FCM message received: ${message.messageId}',
    name: 'FoodyaFCM',
  );
}

class FcmNotificationService {
  FcmNotificationService({
    required String baseUrl,
    required http.Client client,
    required AuthSessionRecovery sessionRecovery,
    FirebaseMessaging? messaging,
    FlutterLocalNotificationsPlugin? localNotifications,
  }) : _baseUrl = baseUrl,
       _client = client,
       _sessionRecovery = sessionRecovery,
       _messaging = messaging ?? FirebaseMessaging.instance,
       _localNotifications =
           localNotifications ?? FlutterLocalNotificationsPlugin();

  final String _baseUrl;
  final http.Client _client;
  final AuthSessionRecovery _sessionRecovery;
  final FirebaseMessaging _messaging;
  final FlutterLocalNotificationsPlugin _localNotifications;

  static const AndroidNotificationChannel _androidChannel =
      AndroidNotificationChannel(
        'foodya_order_updates',
        'Foodya order updates',
        description: 'Order and account notifications for Foodya users.',
        importance: Importance.high,
      );

  Future<void> initialize() async {
    await _configureLocalNotifications();
    await _requestPermissions();
    await _logCurrentToken();

    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);
    FirebaseMessaging.onMessageOpenedApp.listen(_handleNotificationOpen);

    final initialMessage = await _messaging.getInitialMessage();
    if (initialMessage != null) {
      _handleNotificationOpen(initialMessage);
    }

    _messaging.onTokenRefresh.listen((token) {
      developer.log('FCM token refreshed: $token', name: 'FoodyaFCM');
      unawaited(_registerToken(token));
    });
  }

  Future<void> registerCurrentDevice() async {
    try {
      final token = await _messaging.getToken();
      developer.log('FCM token: $token', name: 'FoodyaFCM');
      if (token == null || token.trim().isEmpty) {
        return;
      }
      await _registerToken(token);
    } catch (error, stackTrace) {
      developer.log(
        'Unable to register FCM token.',
        name: 'FoodyaFCM',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  Future<void> _logCurrentToken() async {
    try {
      final token = await _messaging.getToken();
      developer.log('FCM token: $token', name: 'FoodyaFCM');
    } catch (error, stackTrace) {
      developer.log(
        'Unable to get FCM token.',
        name: 'FoodyaFCM',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  Future<void> unregisterCurrentDevice() async {
    try {
      final token = await _messaging.getToken();
      if (token == null || token.trim().isEmpty) {
        return;
      }
      await _unregisterToken(token);
    } catch (error, stackTrace) {
      developer.log(
        'Unable to unregister FCM token.',
        name: 'FoodyaFCM',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  Future<void> _configureLocalNotifications() async {
    const androidSettings = AndroidInitializationSettings(
      '@mipmap/ic_launcher',
    );
    const settings = InitializationSettings(android: androidSettings);
    await _localNotifications.initialize(settings);

    final androidPlugin =
        _localNotifications
            .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin
            >();
    await androidPlugin?.createNotificationChannel(_androidChannel);
  }

  Future<void> _requestPermissions() async {
    final settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );
    developer.log(
      'FCM permission status: ${settings.authorizationStatus.name}',
      name: 'FoodyaFCM',
    );
  }

  Future<void> _registerToken(String token) {
    return _sendAuthorized(
      method: 'POST',
      path: '/api/v1/notifications/devices',
      body: {'token': token, 'platform': _platformName()},
    );
  }

  Future<void> _unregisterToken(String token) {
    return _sendAuthorized(
      method: 'DELETE',
      path: '/api/v1/notifications/devices',
      body: {'token': token},
    );
  }

  Future<void> _sendAuthorized({
    required String method,
    required String path,
    required Map<String, String> body,
  }) {
    return _sessionRecovery.runAuthorized((accessToken) async {
      final response = await _client
          .send(
            http.Request(method, Uri.parse('$_baseUrl$path'))
              ..headers.addAll({
                'Content-Type': 'application/json',
                'Authorization': 'Bearer $accessToken',
              })
              ..body = jsonEncode(body),
          )
          .timeout(const Duration(seconds: 12));

      final responseBody = await response.stream.bytesToString();
      if (response.statusCode >= 200 && response.statusCode < 300) {
        developer.log(
          'FCM device token sync succeeded: $method $path',
          name: 'FoodyaFCM',
        );
        return;
      }

      final decoded = _tryDecodeMap(responseBody);
      throw ApiException(
        statusCode: response.statusCode,
        code: decoded?['code']?.toString(),
        message:
            decoded?['message']?.toString() ??
            'Device token sync failed with status ${response.statusCode}.',
        details: decoded?['details'],
      );
    });
  }

  String _platformName() {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'ANDROID';
    }
    if (defaultTargetPlatform == TargetPlatform.iOS) {
      return 'IOS';
    }
    if (kIsWeb) {
      return 'WEB';
    }
    return 'UNKNOWN';
  }

  Map<String, dynamic>? _tryDecodeMap(String raw) {
    if (raw.trim().isEmpty) {
      return null;
    }
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
      if (decoded is Map) {
        return decoded.map((key, value) => MapEntry(key.toString(), value));
      }
    } catch (_) {
      return null;
    }
    return null;
  }

  Future<void> _handleForegroundMessage(RemoteMessage message) async {
    developer.log(
      'Foreground FCM message received: ${message.messageId}',
      name: 'FoodyaFCM',
    );

    final notification = message.notification;
    if (notification == null || kIsWeb) {
      return;
    }

    await _localNotifications.show(
      notification.hashCode,
      notification.title,
      notification.body,
      NotificationDetails(
        android: AndroidNotificationDetails(
          _androidChannel.id,
          _androidChannel.name,
          channelDescription: _androidChannel.description,
          importance: Importance.high,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
        ),
      ),
      payload: message.data['orderId']?.toString(),
    );
  }

  void _handleNotificationOpen(RemoteMessage message) {
    developer.log(
      'FCM notification opened: ${message.messageId}, data=${message.data}',
      name: 'FoodyaFCM',
    );
  }
}
