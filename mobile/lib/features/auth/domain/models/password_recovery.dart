class ForgotPasswordResult {
  const ForgotPasswordResult({
    required this.challengeToken,
    required this.deliveryHint,
  });

  final String challengeToken;
  final String deliveryHint;

  factory ForgotPasswordResult.fromJson(Map<String, dynamic> json) {
    return ForgotPasswordResult(
      challengeToken: json['challengeToken']?.toString() ?? '',
      deliveryHint: json['deliveryHint']?.toString() ?? '',
    );
  }
}

class VerifyOtpResult {
  const VerifyOtpResult({required this.resetToken});

  final String resetToken;

  factory VerifyOtpResult.fromJson(Map<String, dynamic> json) {
    return VerifyOtpResult(resetToken: json['resetToken']?.toString() ?? '');
  }
}
