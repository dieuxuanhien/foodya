enum UserRole {
  customer,
  merchant;

  String get apiValue {
    switch (this) {
      case UserRole.customer:
        return 'CUSTOMER';
      case UserRole.merchant:
        return 'MERCHANT';
    }
  }

  String get label {
    switch (this) {
      case UserRole.customer:
        return 'Customer';
      case UserRole.merchant:
        return 'Merchant';
    }
  }

  String get homePath {
    switch (this) {
      case UserRole.customer:
        return '/customer/home';
      case UserRole.merchant:
        return '/merchant/home';
    }
  }

  static UserRole? fromApiValue(String? value) {
    switch (value?.toUpperCase()) {
      case 'CUSTOMER':
        return UserRole.customer;
      case 'MERCHANT':
        return UserRole.merchant;
      default:
        return null;
    }
  }
}
