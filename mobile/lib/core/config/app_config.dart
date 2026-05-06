import 'package:flutter/foundation.dart';

class AppConfig {
  static String get apiBaseUrl {
    const configured = String.fromEnvironment(
      'FOODYA_API_BASE_URL',
      defaultValue: '',
    );

    if (configured.isNotEmpty) {
      return configured;
    }

    if (kIsWeb) {
      return 'http://localhost:8080';
    }

    return defaultTargetPlatform == TargetPlatform.android
        ? 'http://10.0.2.2:8080'
        : 'http://localhost:8080';
  }
}
