class UserProfile {
  const UserProfile({
    required this.id,
    required this.username,
    required this.email,
    required this.phoneNumber,
    required this.fullName,
    this.avatarUrl,
    required this.role,
    required this.status,
  });

  final String id;
  final String username;
  final String email;
  final String phoneNumber;
  final String fullName;
  final String? avatarUrl;
  final String role;
  final String status;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id']?.toString() ?? '',
      username: json['username']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      avatarUrl: _nullableText(json['avatarUrl']),
      role: json['role']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
    );
  }

  static String? _nullableText(Object? value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
  }
}
