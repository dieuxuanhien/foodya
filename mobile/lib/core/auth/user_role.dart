enum UserRole {
  customer,
  merchant;

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
}
