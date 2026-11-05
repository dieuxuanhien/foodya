import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/firebase_options.dart';

void main() {
  tearDown(() {
    debugDefaultTargetPlatformOverride = null;
  });

  test('returns Firebase options for the configured Android app', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;

    expect(DefaultFirebaseOptions.isConfiguredForCurrentPlatform, isTrue);
    expect(DefaultFirebaseOptions.currentPlatformOrNull, isNotNull);
  });

  test('returns null for iOS until iOS Firebase options are configured', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;

    expect(DefaultFirebaseOptions.isConfiguredForCurrentPlatform, isFalse);
    expect(DefaultFirebaseOptions.currentPlatformOrNull, isNull);
  });
}
