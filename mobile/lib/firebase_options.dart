// Generated from the Firebase Android app config for local emulator testing.
// Re-run `flutterfire configure` if the Firebase project or app id changes.
import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform, kIsWeb;

class DefaultFirebaseOptions {
  static bool get isConfiguredForCurrentPlatform =>
      currentPlatformOrNull != null;

  static FirebaseOptions? get currentPlatformOrNull {
    if (kIsWeb) {
      return null;
    }

    return switch (defaultTargetPlatform) {
      TargetPlatform.android => android,
      TargetPlatform.iOS ||
      TargetPlatform.macOS ||
      TargetPlatform.windows ||
      TargetPlatform.linux ||
      TargetPlatform.fuchsia => null,
    };
  }

  static FirebaseOptions get currentPlatform {
    final options = currentPlatformOrNull;
    if (options != null) {
      return options;
    }

    throw UnsupportedError(
      'Firebase options are not configured for the current platform.',
    );
  }

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyBKfjhXEloCxZNjHmLGnqYM_ueGPyREZ8Y',
    appId: '1:1067169581597:android:56ea2b09c627645b47d4f4',
    messagingSenderId: '1067169581597',
    projectId: 'foodya-bf729',
    storageBucket: 'foodya-bf729.firebasestorage.app',
  );
}
