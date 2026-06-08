// Generated from the Firebase Android app config for local emulator testing.
// Re-run `flutterfire configure` if the Firebase project or app id changes.
import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform, kIsWeb;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      throw UnsupportedError('Firebase web options are not configured.');
    }

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
      case TargetPlatform.macOS:
      case TargetPlatform.windows:
      case TargetPlatform.linux:
      case TargetPlatform.fuchsia:
        throw UnsupportedError(
          'Firebase options are configured for Android only.',
        );
    }
  }

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyBKfjhXEloCxZNjHmLGnqYM_ueGPyREZ8Y',
    appId: '1:1067169581597:android:56ea2b09c627645b47d4f4',
    messagingSenderId: '1067169581597',
    projectId: 'foodya-bf729',
    storageBucket: 'foodya-bf729.firebasestorage.app',
  );
}
