class AuthFormValidators {
  static final RegExp emailRegex = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');
  static final RegExp phoneRegex = RegExp(r'^\+?[0-9]{9,15}$');
  static final RegExp strongPasswordRegex = RegExp(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$',
  );

  const AuthFormValidators._();

  static String? requiredField(
    String? value, {
    String message = 'This field is required.',
  }) {
    if (value == null || value.trim().isEmpty) {
      return message;
    }
    return null;
  }

  static String? loginIdentity(String? value) {
    return requiredField(value, message: 'Username or email is required.');
  }

  static String? loginPassword(String? value) {
    return requiredField(value, message: 'Password is required.');
  }

  static String? email(String? value) {
    final error = requiredField(value, message: 'Email is required.');
    if (error != null) {
      return error;
    }

    if (!emailRegex.hasMatch(value!.trim())) {
      return 'Please enter a valid email address.';
    }
    return null;
  }

  static String? phoneNumber(String? value) {
    final error = requiredField(value, message: 'Phone number is required.');
    if (error != null) {
      return error;
    }

    if (!phoneRegex.hasMatch(value!.trim())) {
      return 'Phone number must be 9-15 digits and may start with +.';
    }
    return null;
  }

  static String? password(String? value) {
    if (value == null || value.isEmpty) {
      return 'Password is required.';
    }

    if (!strongPasswordRegex.hasMatch(value)) {
      return 'Must be >=8 chars with uppercase, lowercase, number, special char.';
    }
    return null;
  }

  static String? confirmPassword({
    required String? password,
    required String? confirmPassword,
  }) {
    if (confirmPassword == null || confirmPassword.isEmpty) {
      return 'Please confirm your password.';
    }

    if (confirmPassword != password) {
      return 'Confirm password must match password.';
    }

    return null;
  }
}
