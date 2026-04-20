import 'dart:convert';

import 'user_role.dart';

class JwtClaimsDecoder {
  static UserRole? parseRole(String token) {
    final claims = parsePayload(token);
    return UserRole.fromApiValue(claims['role']?.toString());
  }

  static DateTime? parseExpiry(String token) {
    final claims = parsePayload(token);
    final expRaw = claims['exp'];

    if (expRaw is int) {
      return DateTime.fromMillisecondsSinceEpoch(expRaw * 1000, isUtc: true);
    }

    if (expRaw is String) {
      final value = int.tryParse(expRaw);
      if (value == null) {
        return null;
      }
      return DateTime.fromMillisecondsSinceEpoch(value * 1000, isUtc: true);
    }

    return null;
  }

  static bool isExpiringSoon(
    String token, {
    Duration threshold = const Duration(seconds: 30),
  }) {
    final expiresAt = parseExpiry(token);
    if (expiresAt == null) {
      return true;
    }

    return DateTime.now().toUtc().add(threshold).isAfter(expiresAt);
  }

  static Map<String, dynamic> parsePayload(String token) {
    final chunks = token.split('.');
    if (chunks.length < 2) {
      throw const FormatException('Invalid JWT format');
    }

    final normalized = base64Url.normalize(chunks[1]);
    final payload = utf8.decode(base64Url.decode(normalized));
    final decoded = jsonDecode(payload);

    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Invalid JWT payload');
    }

    return decoded;
  }
}
